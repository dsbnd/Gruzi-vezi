package com.rzd.dispatcher.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {

    @NotNull(message = "ID заказа обязателен")
    @JsonProperty("orderId")
    private UUID orderId;

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "Название компании обязательно")
    @Size(max = 255)
    @JsonProperty("companyName")
    private String companyName;

    @NotBlank(message = "ИНН обязателен")
    @Pattern(regexp = "^\\d{10}$|^\\d{12}$", message = "ИНН должен содержать 10 или 12 цифр")
    @JsonProperty("inn")
    private String inn;

    @Pattern(regexp = "^\\d{9}$", message = "КПП должен содержать 9 цифр")
    @JsonProperty("kpp")
    private String kpp;

    @NotBlank(message = "БИК обязателен")
    @Pattern(regexp = "^\\d{9}$", message = "БИК должен содержать 9 цифр")
    @JsonProperty("bik")
    private String bik;

    @NotBlank(message = "Расчетный счет обязателен")
    @Pattern(regexp = "^\\d{20}$", message = "Расчетный счет должен содержать 20 цифр")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("correspondentAccount")
    private String correspondentAccount;

    @NotBlank(message = "Название банка обязательно")
    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("paymentPurpose")
    private String paymentPurpose;
}