package com.rzd.dispatcher.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("payment_id")
    private String paymentId;

    private BigDecimal amount;
    private String status;

    @JsonProperty("payment_method")
    private String paymentMethod;

    // Корпоративные поля
    @JsonProperty("company_name")
    private String companyName;

    private String inn;
    private String kpp;
    private String bik;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("payment_document")
    private String paymentDocument;

    @JsonProperty("payment_purpose")
    private String paymentPurpose;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("paid_at")
    private OffsetDateTime paidAt;
}