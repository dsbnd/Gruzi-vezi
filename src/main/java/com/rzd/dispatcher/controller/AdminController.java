package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.response.OrderResponse;
import com.rzd.dispatcher.model.dto.response.PaymentResponse;
import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.entity.Wagon;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.model.enums.WagonStatus;
import com.rzd.dispatcher.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // допзащита на уровне контроллера
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final WagonAdminService wagonAdminService;
    private final PaymentService paymentService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Пользователь удален");
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {
        orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok("Статус заявки изменен на: " + status);
    }

    @GetMapping("/wagons")
    public ResponseEntity<List<Wagon>> getAllWagons() {
        return ResponseEntity.ok(wagonAdminService.getAllWagons());
    }

    @PostMapping("/wagons")
    public ResponseEntity<Wagon> addWagon(@RequestBody Wagon wagon) {
        return ResponseEntity.ok(wagonAdminService.addWagon(wagon));
    }

    @PutMapping("/wagons/{id}/status")
    public ResponseEntity<String> updateWagonStatus(
            @PathVariable UUID id, 
            @RequestParam WagonStatus status) {
        wagonAdminService.updateStatus(id, status);
        return ResponseEntity.ok("Статус вагона обновлен");
    }

    @DeleteMapping("/wagons/{id}")
    public ResponseEntity<String> deleteWagon(@PathVariable UUID id) {
        wagonAdminService.deleteWagon(id);
        return ResponseEntity.ok("Вагон удален");
    }
    
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/payments/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }
}