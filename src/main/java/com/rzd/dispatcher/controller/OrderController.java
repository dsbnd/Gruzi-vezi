package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
import com.rzd.dispatcher.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
}