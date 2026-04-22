package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.entity.CompanyAccount;
import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.entity.Payment;
import com.rzd.dispatcher.model.entity.Payment.PaymentStatus;
import com.rzd.dispatcher.model.dto.request.PaymentRequest;
import com.rzd.dispatcher.model.dto.request.PaymentWebhookRequest;
import com.rzd.dispatcher.model.dto.response.PaymentResponse;
import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.repository.CompanyAccountRepository;
import com.rzd.dispatcher.repository.OrderRepository;
import com.rzd.dispatcher.repository.PaymentRepository;
import com.rzd.dispatcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rzd.dispatcher.service.AccountService.TransferResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PdfGeneratorService pdfGeneratorService;
    private final CompanyAccountRepository accountRepository;
    private final AccountService accountService;

    private static final String PAYMENT_IDEMPOTENCY_KEY = "payment:processed:";
    private static final String PAYMENT_INN_CACHE_KEY = "payments:inn:";

    @Transactional
    public Payment createCorporatePayment(PaymentRequest request, String userEmail) {
        log.info("Создание платежа: ИНН={}, сумма={}, введенный счет={}, назначение={}",
                request.getInn(), request.getAmount(), request.getAccountNumber(),
                request.getPaymentPurpose());

        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if ("INDIVIDUAL".equals(user.getUserType())) {
            return createIndividualPayment(request, user);
        } else {
            return createLegalEntityPayment(request, user);
        }
    }

    private Payment createIndividualPayment(PaymentRequest request, User user) {
        log.info("Создание платежа для физического лица: {}", user.getDisplayName());

        
        boolean orderPaid = orderRepository.findById(request.getOrderId())
                .map(order -> order.getStatus() == OrderStatus.оплачен)
                .orElse(false);

        if (orderPaid) {
            throw new RuntimeException("Заказ уже оплачен");
        }

        
        CompanyAccount payerAccount = null;

        
        if (request.getAccountNumber() != null && !request.getAccountNumber().isEmpty()) {
            payerAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                    .orElse(null);
        }

        
        if (payerAccount == null) {
            log.info("Счет для физлица {} не найден, создаем новый", user.getDisplayName());

            
            if (request.getBik() == null || request.getBik().isEmpty()) {
                throw new RuntimeException("Для оплаты необходимо указать БИК банка");
            }
            if (request.getBankName() == null || request.getBankName().isEmpty()) {
                throw new RuntimeException("Для оплаты необходимо указать название банка");
            }

            payerAccount = accountService.createAccount(
                    user.getInn(),
                    user.getCompanyName(),
                    request.getBik(),
                    request.getBankName(),
                    false
            );

            log.info("Создан новый счет для физлица: {}, баланс: {} руб",
                    payerAccount.getAccountNumber(), payerAccount.getBalance());
        }

        
        if (payerAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(String.format(
                    "Недостаточно средств на счете %s. Доступно: %.2f руб, требуется: %.2f руб. Пожалуйста, пополните счет.",
                    payerAccount.getAccountNumber(), payerAccount.getBalance(), request.getAmount()));
        }

        
        CompanyAccount rzdAccount = accountRepository.findByIsRzdAccountTrue()
                .orElseThrow(() -> new RuntimeException("Счет РЖД не найден в базе данных"));

        log.info("Счет РЖД для зачисления: {}, баланс до операции: {} руб",
                rzdAccount.getAccountNumber(), rzdAccount.getBalance());
        log.info("Счет плательщика: {}, баланс до операции: {} руб",
                payerAccount.getAccountNumber(), payerAccount.getBalance());

        
        TransferResult transfer = accountService.transferMoney(
                payerAccount.getAccountNumber(),
                rzdAccount.getAccountNumber(),
                request.getAmount(),
                "Оплата грузовой перевозки (физлицо): " + request.getPaymentPurpose()
        );

        if (!transfer.isSuccess()) {
            throw new RuntimeException("Ошибка перевода средств: " + transfer.getMessage());
        }

        
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaymentMethod("BANK_TRANSFER");

        
        payment.setCompanyName(user.getDisplayName());
        payment.setInn(user.getInn());
        payment.setPaymentPurpose(request.getPaymentPurpose());

        
        payment.setBik(payerAccount.getBik());
        payment.setAccountNumber(payerAccount.getAccountNumber());
        payment.setBankName(payerAccount.getBankName());

        
        payment.setPaymentId(generatePaymentId());
        payment.setPaymentDocument(generatePaymentDocumentNumber());
        payment.setPaymentDate(OffsetDateTime.now());
        payment.setPaidAt(OffsetDateTime.now());

        
        String metadata = String.format(
                "Перевод со счета физлица %s (баланс был: %.2f, стал: %.2f) на счет РЖД %s (баланс был: %.2f, стал: %.2f). Сумма: %.2f. ФИО: %s, ИНН: %s",
                transfer.getFromAccountNumber(),
                transfer.getFromBalanceBefore(),
                transfer.getFromBalanceAfter(),
                transfer.getToAccountNumber(),
                transfer.getToBalanceBefore(),
                transfer.getToBalanceAfter(),
                transfer.getAmount(),
                user.getDisplayName(),
                user.getInn()
        );
        payment.setMetadata(metadata);

        Payment savedPayment = paymentRepository.save(payment);

        
        if (savedPayment.getOrderId() != null) {
            orderRepository.findById(savedPayment.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.оплачен);
                orderRepository.save(order);
                log.info("Статус заказа {} обновлен на 'оплачен'", savedPayment.getOrderId());
            });
        }

        
        cachePaymentByInn(savedPayment);

        log.info("Платеж для физлица успешно создан и оплачен: id={}, документ={}, сумма={}, статус={}",
                savedPayment.getId(), savedPayment.getPaymentDocument(),
                savedPayment.getAmount(), savedPayment.getStatus());

        return savedPayment;
    }

    private Payment createLegalEntityPayment(PaymentRequest request, User user) {
        log.info("Создание платежа для юридического лица: {}", user.getCompanyName());

        boolean orderPaid = orderRepository.findById(request.getOrderId())
                .map(order -> order.getStatus() == OrderStatus.оплачен)
                .orElse(false);

        if (orderPaid) {
            throw new RuntimeException("Заказ уже оплачен");
        }

        CompanyAccount payerAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElse(null);

        if (payerAccount == null) {
            log.info("Счет {} не найден в системе, создаем новый для компании {} (ИНН: {})",
                    request.getAccountNumber(), user.getCompanyName(), user.getInn());

            if (request.getBik() == null || request.getBik().isEmpty()) {
                throw new RuntimeException("Для создания нового счета необходимо указать БИК");
            }
            if (request.getBankName() == null || request.getBankName().isEmpty()) {
                throw new RuntimeException("Для создания нового счета необходимо указать название банка");
            }

            payerAccount = accountService.createAccount(
                    user.getInn(),
                    user.getCompanyName(),
                    request.getBik(),
                    request.getBankName(),
                    false
            );

            log.info("Новый счет создан: {}, баланс: {} руб",
                    payerAccount.getAccountNumber(), payerAccount.getBalance());
        } else {
            log.info("Счет {} найден в системе, баланс: {} руб",
                    request.getAccountNumber(), payerAccount.getBalance());

            if (!payerAccount.getInn().equals(user.getInn())) {
                throw new RuntimeException(
                        "Счет " + request.getAccountNumber() + " не принадлежит вашей компании. " +
                                "Укажите счет, зарегистрированный на ИНН " + user.getInn());
            }
        }

        if (payerAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(String.format(
                    "Недостаточно средств на счете %s. Доступно: %.2f руб, требуется: %.2f руб",
                    request.getAccountNumber(), payerAccount.getBalance(), request.getAmount()));
        }

        CompanyAccount rzdAccount = accountRepository.findByIsRzdAccountTrue()
                .orElseThrow(() -> new RuntimeException("Счет РЖД не найден в базе данных"));

        log.info("Счет РЖД для зачисления: {}, баланс до операции: {} руб",
                rzdAccount.getAccountNumber(), rzdAccount.getBalance());

        TransferResult transfer = accountService.transferMoney(
                payerAccount.getAccountNumber(),
                rzdAccount.getAccountNumber(),
                request.getAmount(),
                "Оплата грузовой перевозки: " + request.getPaymentPurpose()
        );

        if (!transfer.isSuccess()) {
            throw new RuntimeException("Ошибка перевода: " + transfer.getMessage());
        }

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaymentMethod(request.getPaymentMethod());

        payment.setCompanyName(user.getCompanyName());
        payment.setInn(user.getInn());
        payment.setKpp(request.getKpp());
        payment.setBik(payerAccount.getBik());
        payment.setAccountNumber(payerAccount.getAccountNumber());
        payment.setCorrespondentAccount(request.getCorrespondentAccount());
        payment.setBankName(payerAccount.getBankName());
        payment.setPaymentPurpose(request.getPaymentPurpose());

        payment.setPaymentId(generatePaymentId());
        payment.setPaymentDocument(generatePaymentDocumentNumber());
        payment.setPaymentDate(OffsetDateTime.now());
        payment.setPaidAt(OffsetDateTime.now());

        String metadata = String.format(
                "Перевод со счета %s (баланс был: %.2f, стал: %.2f) на счет РЖД %s (баланс был: %.2f, стал: %.2f). Сумма: %.2f",
                transfer.getFromAccountNumber(),
                transfer.getFromBalanceBefore(),
                transfer.getFromBalanceAfter(),
                transfer.getToAccountNumber(),
                transfer.getToBalanceBefore(),
                transfer.getToBalanceAfter(),
                transfer.getAmount()
        );
        payment.setMetadata(metadata);

        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.getOrderId() != null) {
            orderRepository.findById(savedPayment.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.оплачен);
                orderRepository.save(order);
                log.info("Статус заказа {} обновлен на 'оплачен'", savedPayment.getOrderId());
            });
        }

        cachePaymentByInn(savedPayment);

        log.info("Платеж успешно создан: id={}, документ={}, сумма={}, статус={}",
                savedPayment.getId(), savedPayment.getPaymentDocument(),
                savedPayment.getAmount(), savedPayment.getStatus());

        return savedPayment;
    }

    @Transactional
    public PaymentResponse confirmIndividualPayment(String paymentDocument, BigDecimal amount, String inn) {
        log.info("Подтверждение оплаты от физлица: документ={}, сумма={}, ИНН={}",
                paymentDocument, amount, inn);

        Payment payment = paymentRepository.findByPaymentDocument(paymentDocument)
                .orElseThrow(() -> new RuntimeException("Платеж с документом " + paymentDocument + " не найден"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Платеж уже обработан. Статус: " + payment.getStatus());
        }

        
        if (payment.getAmount().compareTo(amount) != 0) {
            throw new RuntimeException("Сумма платежа не совпадает. Ожидалось: " +
                    payment.getAmount() + ", получено: " + amount);
        }

        
        if (!payment.getInn().equals(inn)) {
            throw new RuntimeException("ИНН плательщика не совпадает");
        }

        
        CompanyAccount rzdAccount = accountRepository.findByIsRzdAccountTrue()
                .orElseThrow(() -> new RuntimeException("Счет РЖД не найден в базе данных"));

        BigDecimal beforeBalance = rzdAccount.getBalance();
        log.info("Баланс счета РЖД до зачисления: {} руб", beforeBalance);

        
        int deposited = accountRepository.deposit(rzdAccount.getAccountNumber(), payment.getAmount());

        if (deposited == 0) {
            throw new RuntimeException("Ошибка при зачислении средств на счет РЖД");
        }

        
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(OffsetDateTime.now());
        payment.setPaymentDate(OffsetDateTime.now());

        
        CompanyAccount updatedRzdAccount = accountRepository.findByAccountNumber(rzdAccount.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Счет РЖД не найден"));

        
        String metadata = String.format(
                "Зачисление от физлица %s (ИНН: %s). Сумма: %.2f. Баланс РЖД был: %.2f, стал: %.2f",
                payment.getCompanyName(),
                payment.getInn(),
                payment.getAmount(),
                beforeBalance,
                updatedRzdAccount.getBalance()
        );

        String currentMetadata = payment.getMetadata() != null ? payment.getMetadata() : "";
        payment.setMetadata(currentMetadata + "\n" + metadata);

        Payment savedPayment = paymentRepository.save(payment);

        
        if (savedPayment.getOrderId() != null) {
            orderRepository.findById(savedPayment.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.оплачен);
                orderRepository.save(order);
                log.info("Статус заказа {} обновлен на 'оплачен'", savedPayment.getOrderId());
            });
        }

        
        cachePaymentByInn(savedPayment);

        log.info("Платеж от физлица подтвержден: {}, сумма {} руб зачислена на счет РЖД. Новый баланс РЖД: {} руб",
                savedPayment.getPaymentDocument(),
                savedPayment.getAmount(),
                updatedRzdAccount.getBalance());

        return convertToResponse(savedPayment);
    }

    private String generatePaymentId() {
        return "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generatePaymentDocumentNumber() {
        return String.format("РЖД-%d-%03d",
                System.currentTimeMillis() / 1000,
                (int)(Math.random() * 1000));
    }

    private void cachePaymentByInn(Payment payment) {
        String key = PAYMENT_INN_CACHE_KEY + payment.getInn();
        redisTemplate.opsForList().leftPush(key, payment.getId().toString());
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    @Transactional
    public PaymentResponse handleBankWebhook(PaymentWebhookRequest request) {
        log.info("Обработка банковского вебхука: paymentId={}, status={}, inn={}",
                request.getPaymentId(), request.getStatus(), request.getInn());

        String idempotencyKey = PAYMENT_IDEMPOTENCY_KEY + request.getPaymentId();
        Boolean isProcessed = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "processed", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isProcessed)) {
            log.warn("Платеж {} уже был обработан", request.getPaymentId());
            throw new RuntimeException("Платеж уже обработан");
        }

        try {
            Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                    .orElseGet(() -> {
                        if (request.getInn() != null && request.getAmount() != null) {
                            return paymentRepository.findByInnAndAmountAndStatus(
                                    request.getInn(),
                                    request.getAmount(),
                                    PaymentStatus.PENDING
                            ).orElseThrow(() -> new RuntimeException("Платеж не найден"));
                        }
                        throw new RuntimeException("Недостаточно данных для поиска платежа");
                    });

            switch (request.getStatus()) {
                case "succeeded":
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setPaidAt(OffsetDateTime.now());
                    payment.setPaymentDate(request.getPaymentDate());

                    if (request.getPaymentDocument() != null) {
                        payment.setPaymentDocument(request.getPaymentDocument());
                    }

                    log.info("Платеж от {} (ИНН: {}) успешно завершен",
                            payment.getCompanyName(), payment.getInn());

                    if (payment.getOrderId() != null) {
                        Order order = orderRepository.findById(payment.getOrderId())
                                .orElseThrow(() -> new RuntimeException("Заказ не найден с ID: " + payment.getOrderId()));

                        OrderStatus oldStatus = order.getStatus();
                        order.setStatus(OrderStatus.оплачен);
                        orderRepository.save(order);

                        log.info("Статус заказа {} обновлен с {} на оплачен",
                                payment.getOrderId(), oldStatus);
                    }
                    break;

                case "processing":
                    payment.setStatus(PaymentStatus.PROCESSING);
                    log.info("Платеж {} в обработке банка", request.getPaymentId());
                    break;

                case "failed":
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setErrorMessage(request.getErrorMessage());
                    log.warn("Платеж {} не удался: {}", request.getPaymentId(), request.getErrorMessage());
                    break;

                case "refunded":
                    payment.setStatus(PaymentStatus.REFUNDED);
                    log.info("Платеж {} возвращен", request.getPaymentId());
                    break;
            }

            if (request.getPaymentId() != null) {
                payment.setPaymentId(request.getPaymentId());
            }

            Payment savedPayment = paymentRepository.save(payment);
            return convertToResponse(savedPayment);

        } catch (Exception e) {
            redisTemplate.delete(idempotencyKey);
            throw e;
        }
    }

    @Transactional
    public void sendSuccessWebhook(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));

        PaymentWebhookRequest webhook = new PaymentWebhookRequest();
        webhook.setPaymentId("bank_real_" + System.currentTimeMillis());
        webhook.setOrderId(payment.getOrderId());
        webhook.setStatus("succeeded");
        webhook.setAmount(payment.getAmount());
        webhook.setInn(payment.getInn());
        webhook.setCompanyName(payment.getCompanyName());
        webhook.setPaymentDocument("ПП-" + System.currentTimeMillis());
        webhook.setPaymentDate(OffsetDateTime.now());

        PaymentResponse response = handleBankWebhook(webhook);
        log.info("Вебхук об успешной оплате отправлен для платежа {}", paymentId);
    }

    @Transactional(readOnly = true)
    public boolean isOrderPaid(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(order -> order.getStatus() == OrderStatus.оплачен)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOrderPaidByPayments(UUID orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findPaymentsByInn(String inn) {
        log.info("Поиск платежей по ИНН: {}", inn);

        String key = PAYMENT_INN_CACHE_KEY + inn;
        List<String> paymentIds = redisTemplate.opsForList().range(key, 0, -1);

        if (paymentIds != null && !paymentIds.isEmpty()) {
            log.info("Найдено {} платежей в Redis", paymentIds.size());
            return paymentIds.stream()
                    .map(UUID::fromString)
                    .map(id -> paymentRepository.findById(id).orElse(null))
                    .filter(p -> p != null)
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        List<Payment> payments = paymentRepository.findByInn(inn);
        payments.forEach(this::cachePaymentByInn);

        return payments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));
        return convertToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse convertToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .companyName(payment.getCompanyName())
                .inn(payment.getInn())
                .kpp(payment.getKpp())
                .bik(payment.getBik())
                .accountNumber(payment.getAccountNumber())
                .bankName(payment.getBankName())
                .paymentDocument(payment.getPaymentDocument())
                .paymentPurpose(payment.getPaymentPurpose())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public byte[] generateInvoicePdf(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));
        log.info("Генерация PDF для платежа {}. Сумма в БД: {}", paymentId, payment.getAmount());
        try {
            return pdfGeneratorService.generateInvoicePdf(payment);
        } catch (Exception e) {
            log.error("Ошибка при генерации PDF для платежа {}", paymentId, e);
            throw new RuntimeException("Не удалось создать PDF документ");
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        log.info("Инициация возврата для платежа: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new RuntimeException("Возврат возможен только для успешно завершенных платежей. Текущий статус: " + payment.getStatus());
        }

        CompanyAccount rzdAccount = accountRepository.findByIsRzdAccountTrue()
                .orElseThrow(() -> new RuntimeException("Счет РЖД не найден в базе данных"));

        TransferResult transfer = accountService.transferMoney(
                rzdAccount.getAccountNumber(),
                payment.getAccountNumber(),
                payment.getAmount(),
                "Возврат средств по отмененному платежу: " + payment.getPaymentId()
        );

        if (!transfer.isSuccess()) {
            throw new RuntimeException("Ошибка возврата средств: " + transfer.getMessage());
        }

        payment.setStatus(PaymentStatus.REFUNDED);

        String refundMetadata = String.format(
                "\n[ВОЗВРАТ %s] Перевод со счета РЖД %s на счет %s. Сумма: %.2f",
                OffsetDateTime.now().toString(),
                transfer.getFromAccountNumber(),
                transfer.getToAccountNumber(),
                transfer.getAmount()
        );

        String currentMetadata = payment.getMetadata() != null ? payment.getMetadata() : "";
        payment.setMetadata(currentMetadata + refundMetadata);

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Возврат успешно проведен. Платеж {} переведен в статус REFUNDED", payment.getId());

        if (updatedPayment.getOrderId() != null) {
            orderRepository.findById(updatedPayment.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.ожидает_оплаты);
                orderRepository.save(order);
                log.info("Статус заказа {} изменен на 'ожидает_оплаты'", order.getId());
            });
        }

        return convertToResponse(updatedPayment);
    }

    public void refundPaymentByOrderId(UUID orderId) {
        log.info("Возврат средств для заказа: {}", orderId);


        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.SUCCEEDED)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Успешный платеж для заказа не найден"));

        refundPayment(payment.getId());
    }
}