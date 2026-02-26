package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.PaymentWebhookRequest;
import com.rzd.dispatcher.model.dto.response.PaymentResponse;
import com.rzd.dispatcher.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatcher/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestParam UUID orderId,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod) {

        com.rzd.dispatcher.model.entity.Payment payment = paymentService.createPayment(
                orderId, amount, paymentMethod);

        return ResponseEntity.ok(convertToResponse(payment));
    }

    @PostMapping("/webhook")
    public ResponseEntity<PaymentResponse> handleWebhook(
            @RequestBody PaymentWebhookRequest request) {
        PaymentResponse response = paymentService.handleWebhook(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(
            @PathVariable UUID orderId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/order/{orderId}/paid")
    public ResponseEntity<Boolean> isOrderPaid(@PathVariable UUID orderId) {
        boolean paid = paymentService.isOrderPaid(orderId);
        return ResponseEntity.ok(paid);
    }

    private PaymentResponse convertToResponse(com.rzd.dispatcher.model.entity.Payment payment) {
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