package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.PaymentRequest;
import com.rzd.dispatcher.model.dto.request.PaymentWebhookRequest;
import com.rzd.dispatcher.model.dto.response.PaymentResponse;
import com.rzd.dispatcher.service.PaymentService;
import jakarta.validation.Valid;
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


    @PostMapping("/corporate")
    public ResponseEntity<PaymentResponse> createCorporatePayment(
            @Valid @RequestBody PaymentRequest request) {

        com.rzd.dispatcher.model.entity.Payment payment =
                paymentService.createCorporatePayment(request);

        return ResponseEntity.ok(convertToResponse(payment));
    }


    @GetMapping("/search/by-inn")
    public ResponseEntity<List<PaymentResponse>> findPaymentsByInn(
            @RequestParam String inn) {

        List<PaymentResponse> payments = paymentService.findPaymentsByInn(inn);
        return ResponseEntity.ok(payments);
    }


    @GetMapping("/{paymentId}/invoice")
    public ResponseEntity<String> generateInvoice(@PathVariable UUID paymentId) {
        String invoice = paymentService.generateInvoice(paymentId);
        return ResponseEntity.ok(invoice);
    }


    @PostMapping("/bank-webhook")
    public ResponseEntity<PaymentResponse> handleBankWebhook(
            @RequestBody PaymentWebhookRequest request) {
        PaymentResponse response = paymentService.handleBankWebhook(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение платежей по заказу (работает)
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(
            @PathVariable UUID orderId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Проверка оплаты заказа (работает)
     */
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
}