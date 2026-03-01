package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
import com.rzd.dispatcher.model.dto.response.OrderResponse;
import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.repository.OrderRepository;
import com.rzd.dispatcher.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        // Достаем email из JWT токена
        String userEmail = authentication.getName();
        
        UUID orderId = orderService.createDraftOrder(request, userEmail);
        
        return ResponseEntity.ok(Map.of(
                "orderId", orderId, 
                "message", "Заявка (черновик) успешно создана"
        ));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        // Достаем email из токена
        String email = authentication.getName();

        // Достаем из БД все заявки этого пользователя
        List<Order> userOrders = orderRepository.findByUser_Email(email);

        // Превращаем их в DTO, используя ТВОЙ готовый метод fromOrder!
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


}