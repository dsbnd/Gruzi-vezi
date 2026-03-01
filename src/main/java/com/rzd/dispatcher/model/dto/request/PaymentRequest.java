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

    // Эти поля теперь опциональны, так как берутся из БД пользователя
    @Size(max = 255, message = "Название компании не должно превышать 255 символов")
    @JsonProperty("companyName")
    private String companyName;  // Будет проигнорировано

    @Pattern(regexp = "^\\d{10}$|^\\d{12}$", message = "ИНН должен содержать 10 или 12 цифр")
    @JsonProperty("inn")
    private String inn;  // Будет проигнорировано

    @Pattern(regexp = "^\\d{9}$", message = "КПП должен содержать 9 цифр")
    @JsonProperty("kpp")
    private String kpp;

    @Pattern(regexp = "^\\d{9}$", message = "БИК должен содержать 9 цифр")
    @JsonProperty("bik")
    private String bik;  // Используется только при создании нового счета

    @NotBlank(message = "Номер счета обязателен для оплаты")
    @Pattern(regexp = "^\\d{20}$", message = "Расчетный счет должен содержать 20 цифр")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("correspondentAccount")
    private String correspondentAccount;

    @Size(max = 255, message = "Название банка не должно превышать 255 символов")
    @JsonProperty("bankName")
    private String bankName;  // Используется только при создании нового счета

    @NotBlank(message = "Назначение платежа обязательно")
    @Size(max = 500, message = "Назначение платежа не должно превышать 500 символов")
    @JsonProperty("paymentPurpose")
    private String paymentPurpose;

    // ДОБАВЛЯЕМ поле paymentMethod
    @JsonProperty("paymentMethod")
    private String paymentMethod;  // Например: "BANK_TRANSFER", "CARD", etc.
}