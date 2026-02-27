package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
import com.rzd.dispatcher.model.entity.Cargo;
import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.repository.OrderRepository;
import com.rzd.dispatcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

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
}