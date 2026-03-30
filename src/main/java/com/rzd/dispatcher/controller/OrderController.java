package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
import com.rzd.dispatcher.model.dto.response.OrderResponse;
import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.repository.OrderRepository;
import com.rzd.dispatcher.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        
        UUID orderId = orderService.createDraftOrder(request, userEmail);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId, 
                "message", "Заявка (черновик) успешно создана"
        ));
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден с ID: " + orderId));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("У вас нет доступа к этому заказу");
        }
        return ResponseEntity.ok(OrderResponse.fromOrder(order));
    }
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        List<Order> userOrders = orderRepository.findByUser_Email(email);
        List<OrderResponse> responseList = userOrders.stream()
                .map(OrderResponse::fromOrder)
                .toList();

        return ResponseEntity.ok(responseList);
    }



    @GetMapping("/{orderId}/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable UUID orderId) {
        byte[] pdfContent = orderService.generateOrderContract(orderId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"contract_" + orderId + ".pdf\"")
                .body(pdfContent);
    }


    @PostMapping("/{orderId}/confirm-wagon")
    public ResponseEntity<OrderResponse> confirmWagon(
            @PathVariable UUID orderId,
            @RequestParam UUID wagonId,
            @RequestParam BigDecimal totalPrice,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        Order updatedOrder = orderService.confirmWagonSelection(orderId, wagonId, totalPrice, userEmail);
        return ResponseEntity.ok(OrderResponse.fromOrder(updatedOrder));
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderResponse>> getAllOrdersForAdmin() {
        List<Order> allOrders = orderRepository.findAll();
        List<OrderResponse> responseList = allOrders.stream()
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam String newStatus
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден с ID: " + orderId));
        order.setStatus(OrderStatus.valueOf(newStatus));
        orderRepository.save(order);

        return ResponseEntity.ok(OrderResponse.fromOrder(order));
    }

}