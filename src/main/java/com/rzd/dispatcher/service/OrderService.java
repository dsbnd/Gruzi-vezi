package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
import com.rzd.dispatcher.model.dto.response.OrderResponse;
import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.model.entity.Cargo;
import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.entity.Wagon;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.repository.OrderRepository;
import com.rzd.dispatcher.repository.UserRepository;
import com.rzd.dispatcher.repository.WagonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WagonRepository wagonRepository;
    private final OrderValidator orderValidator;
    private final PdfGeneratorService pdfGeneratorService;


    private final PricingService pricingService;
    private final WagonSearchService wagonSearchService;
    private final PaymentService paymentService;


    public UUID createDraftOrder(CreateOrderRequest request, String userEmail) {
        orderValidator.validate(request);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Order order = new Order();
        order.setUser(user);
        order.setDepartureStation(request.getDepartureStation());
        order.setDestinationStation(request.getDestinationStation());
        order.setRequestedWagonType(request.getRequestedWagonType());
        order.setStatus(OrderStatus.черновик);

        Cargo cargo = new Cargo();
        cargo.setCargoType(request.getCargo().getCargoType());
        cargo.setWeightKg(request.getCargo().getWeightKg());
        cargo.setVolumeM3(request.getCargo().getVolumeM3());
        cargo.setPackagingType(request.getCargo().getPackagingType());

        order.setCargo(cargo);
        cargo.setOrder(order);

        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден с ID: " + orderId));

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Статус заказа {} обновлен на: {}", orderId, newStatus);
    }

    @Transactional(readOnly = true)
    public byte[] generateOrderContract(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        try {
            return pdfGeneratorService.generateContractPdf(order);
        } catch (Exception e) {
            log.error("Ошибка при создании договора для заказа {}", orderId, e);
            throw new RuntimeException("Ошибка генерации PDF");
        }
    }

    public Order confirmWagonSelection(UUID orderId, UUID wagonId, BigDecimal totalPrice, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Нет доступа к заказу");
        }

        Wagon wagon = wagonRepository.findById(wagonId)
                .orElseThrow(() -> new RuntimeException("Вагон не найден"));

        order.setWagon(wagon);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.ожидает_оплаты);

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::fromOrder)
                .toList();
    }

    // Транзакция - создание заявки
    @Transactional(rollbackFor = Exception.class)
    public UUID createCompleteOrderWithReservation(
            CreateOrderRequest request,
            String userEmail,
            UUID wagonId,
            Set<String> selectedServices) {

        log.info("Транзакция - создание заявки");
        log.info("Пользователь: {}", userEmail);
        log.info("Вагон ID: {}", wagonId);
        log.info("Станция отправления: {}", request.getDepartureStation());
        log.info("Станция назначения: {}", request.getDestinationStation());

        try {
            log.info("1/5: Создание записи о заявке");
            UUID orderId = createDraftOrder(request, userEmail);
            log.info("1/5: Заявка создана, ID: {}", orderId);

            log.info("2/5: Резервирование вагона");
            boolean reserved = wagonSearchService.reserveWagon(wagonId, orderId, 30);
            if (!reserved) {
                throw new RuntimeException("Не удалось зарезервировать вагон " + wagonId);
            }
            log.info("2/5: Вагон {} зарезервирован", wagonId);


            log.info("3/5: Расчет стоимости перевозки");
            PriceResponse priceResponse = pricingService.calculateFullPrice(orderId, wagonId, selectedServices);
            BigDecimal totalPrice = priceResponse.getTotalPrice();
            log.info("3/5: Стоимость перевозки: {} руб", totalPrice);

            log.info("4/5: Расчет углеродного следа");
            Double carbonFootprint = priceResponse.getCarbonFootprintKg();
            log.info("4/5: Углеродный след: {} кг ", carbonFootprint);

            log.info("5/5: Подтверждение заявки");
            Order confirmedOrder = confirmWagonSelection(orderId, wagonId, totalPrice, userEmail);
            confirmedOrder.setCarbonFootprintKg(BigDecimal.valueOf(carbonFootprint));
            orderRepository.save(confirmedOrder);
            log.info("5/5: Заявка подтверждена, статус: {}", confirmedOrder.getStatus());

            log.info("Транзакция: создание заявки - завершена ");

            return orderId;

        } catch (Exception e) {

            log.info("Транзакция: создание заявки - откатывается ");
            log.error("Причина: {}", e.getMessage());
            throw new RuntimeException("Ошибка при создании заявки: " + e.getMessage(), e);
        }
    }

    // Транзакция - отмена заявки
    @Transactional(rollbackFor = Exception.class)
    public void cancelCompleteOrder(UUID orderId, String userEmail, boolean withRefund) {

        log.info("Транзакция - отмена заявки");
        log.info("Заявка ID: {}", orderId);
        log.info("Пользователь: {}", userEmail);
        log.info("Возврат средств: {}", withRefund);

        try {

            log.info("1/5: Проверка возможности отмены");
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (!order.getUser().getEmail().equals(userEmail)) {
                throw new RuntimeException("У вас нет прав на отмену этой заявки");
            }

            if (order.getStatus() == OrderStatus.в_пути || order.getStatus() == OrderStatus.доставлен) {
                throw new RuntimeException("Невозможно отменить заявку в статусе: " + order.getStatus());
            }
            log.info("1/5: Проверка пройдена, текущий статус: {}", order.getStatus());

            log.info("2/5: Освобождение вагона");
            if (order.getWagon() != null) {
                wagonSearchService.releaseWagon(order.getWagon().getId());
                log.info("2/5: Вагон {} освобожден", order.getWagon().getWagonNumber());
            } else {
                log.info("2/5: Вагон не был зарезервирован");
            }

            log.info("3/5: Возврат денежных средств");
            if (withRefund && order.getStatus() == OrderStatus.оплачен) {
                paymentService.refundPaymentByOrderId(orderId);
                log.info("3/5: Денежные средства возвращены");
            } else if (order.getStatus() == OrderStatus.оплачен && !withRefund) {
                log.info("3/5: Возврат средств не запрошен");
            } else {
                log.info("3/5: Оплата не производилась");
            }

            log.info("4/5: Аннулирование документов");

            log.info("5/5: Изменение статуса заявки");
            order.setStatus(OrderStatus.черновик);
            order.setWagon(null);
            order.setTotalPrice(null);
            orderRepository.save(order);
            log.info("5/5: Статус изменен на: {}", order.getStatus());

            log.info("Транзакция: отмена заявки - завершена ");

        } catch (Exception e) {
            log.info("Транзакция: отмена заявки - откатывается ");
            log.error("Причина: {}", e.getMessage());

            throw new RuntimeException("Ошибка при отмене заявки: " + e.getMessage(), e);
        }
    }
}