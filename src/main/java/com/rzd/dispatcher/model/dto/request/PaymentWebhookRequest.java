package com.rzd.dispatcher.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentWebhookRequest {

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("order_id")
    private UUID orderId;

    private String status;

    private BigDecimal amount;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("error_message")
    private String errorMessage;
}