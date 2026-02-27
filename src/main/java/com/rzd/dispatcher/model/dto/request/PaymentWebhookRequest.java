package com.rzd.dispatcher.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PaymentWebhookRequest {

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("order_id")
    private UUID orderId;

    private String status; // succeeded, failed, refunded, processing

    private BigDecimal amount;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("error_message")
    private String errorMessage;

    // Корпоративные поля (от банка/платежной системы)
    @JsonProperty("inn")
    private String inn;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("kpp")
    private String kpp;

    @JsonProperty("bik")
    private String bik;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("payment_document")
    private String paymentDocument;

    @JsonProperty("payment_date")
    private OffsetDateTime paymentDate;

    @JsonProperty("payment_purpose")
    private String paymentPurpose;

    @JsonProperty("correspondent_account")
    private String correspondentAccount;
}