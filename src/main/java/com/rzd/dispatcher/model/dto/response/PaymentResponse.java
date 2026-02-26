package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private String paymentId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private OffsetDateTime createdAt;
    private OffsetDateTime paidAt;
}