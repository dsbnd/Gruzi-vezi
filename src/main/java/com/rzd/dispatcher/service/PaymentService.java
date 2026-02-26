package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.entity.Payment;
import com.rzd.dispatcher.model.entity.Payment.PaymentStatus;
import com.rzd.dispatcher.model.dto.request.PaymentWebhookRequest;
import com.rzd.dispatcher.model.dto.response.PaymentResponse;
import com.rzd.dispatcher.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PAYMENT_IDEMPOTENCY_KEY = "payment:processed:";

    @Transactional
    public Payment createPayment(UUID orderId, BigDecimal amount, String paymentMethod) {
        log.info("Создание платежа для заказа {} на сумму {}", orderId, amount);

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResponse handleWebhook(PaymentWebhookRequest request) {
        log.info("Обработка вебхука: paymentId={}, status={}",
                request.getPaymentId(), request.getStatus());

        // Проверка на дубликат через Redis
        String idempotencyKey = PAYMENT_IDEMPOTENCY_KEY + request.getPaymentId();
        Boolean isProcessed = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "processed", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isProcessed)) {
            log.warn("Платеж {} уже был обработан", request.getPaymentId());
            throw new RuntimeException("Платеж уже обработан");
        }

        try {
            // Ищем существующий платеж или создаем новый
            Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                    .orElseGet(() -> {
                        Payment newPayment = new Payment();
                        newPayment.setOrderId(request.getOrderId());
                        newPayment.setPaymentId(request.getPaymentId());
                        newPayment.setAmount(request.getAmount());
                        newPayment.setPaymentMethod(request.getPaymentMethod());
                        return newPayment;
                    });

            // Обновляем статус
            switch (request.getStatus()) {
                case "succeeded":
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setPaidAt(OffsetDateTime.now());
                    log.info("Платеж {} успешно завершен", request.getPaymentId());
                    break;

                case "failed":
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setErrorMessage(request.getErrorMessage());
                    log.warn("Платеж {} не удался", request.getPaymentId());
                    break;

                case "refunded":
                    payment.setStatus(PaymentStatus.REFUNDED);
                    log.info("Платеж {} возвращен", request.getPaymentId());
                    break;
            }

            Payment savedPayment = paymentRepository.save(payment);
            return convertToResponse(savedPayment);

        } catch (Exception e) {
            // В случае ошибки удаляем ключ идемпотентности
            redisTemplate.delete(idempotencyKey);
            throw e;
        }
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

    @Transactional(readOnly = true)
    public boolean isOrderPaid(UUID orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED);
    }

    private PaymentResponse convertToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}