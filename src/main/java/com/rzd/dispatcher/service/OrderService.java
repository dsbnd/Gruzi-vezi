package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WagonRepository wagonRepository;

    @Transactional
    public UUID createDraftOrder(CreateOrderRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Order order = new Order();
        order.setUser(user);
        order.setDepartureStation(request.getDepartureStation());
        order.setDestinationStation(request.getDestinationStation());
        order.setRequestedWagonType(request.getRequestedWagonType()); // Сохраняем род вагона
        order.setStatus(OrderStatus.черновик);

        Cargo cargo = new Cargo();
        cargo.setCargoType(request.getCargo().getCargoType());
        cargo.setWeightKg(request.getCargo().getWeightKg());
        cargo.setVolumeM3(request.getCargo().getVolumeM3());
        cargo.setPackagingType(request.getCargo().getPackagingType());

        // Связываем
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
    @Transactional
    public Order confirmWagonSelection(UUID orderId, UUID wagonId, BigDecimal totalPrice, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Проверка принадлежности
        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Нет доступа к заказу");
        }

        // Находим вагон по ID
        Wagon wagon = wagonRepository.findById(wagonId)
                .orElseThrow(() -> new RuntimeException("Вагон не найден"));

        // Устанавливаем объект вагона, а не ID
        order.setWagon(wagon);                    // ← ВОТ ТАК ПРАВИЛЬНО!
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.ожидает_оплаты);

        return orderRepository.save(order);
    }
}