package com.rzd.dispatcher.common.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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


    @NotBlank(message = "Назначение платежа обязательно")
    @Size(max = 500, message = "Назначение платежа не должно превышать 500 символов")
    @JsonProperty("paymentPurpose")
    private String paymentPurpose;


    @NotBlank(message = "Название компании или ФИО обязательно")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    @JsonProperty("companyName")
    private String companyName;

    @NotBlank(message = "ИНН обязателен")
    @Pattern(regexp = "^\\d{10}$|^\\d{12}$", message = "ИНН должен содержать 10 или 12 цифр")
    @JsonProperty("inn")
    private String inn;

    // Физ лицо
    @Pattern(regexp = "^\\d{11}$", message = "СНИЛС должен содержать 11 цифр")
    @JsonProperty("snils")
    private String snils;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{10,20}$", message = "Неверный формат телефона")
    @JsonProperty("phone")
    private String phone;

    @Pattern(regexp = "^\\d{4}$", message = "Серия паспорта должна содержать 4 цифры")
    @JsonProperty("passportSeries")
    private String passportSeries;

    @Pattern(regexp = "^\\d{6}$", message = "Номер паспорта должен содержать 6 цифр")
    @JsonProperty("passportNumber")
    private String passportNumber;

    @Size(max = 255, message = "Кем выдан паспорт не должно превышать 255 символов")
    @JsonProperty("passportIssuedBy")
    private String passportIssuedBy;

    @JsonProperty("passportIssuedDate")
    private String passportIssuedDate;

    @Size(max = 500, message = "Адрес регистрации не должен превышать 500 символов")
    @JsonProperty("registrationAddress")
    private String registrationAddress;

    // Юр лицо
    @Pattern(regexp = "^\\d{9}$", message = "БИК должен содержать 9 цифр")
    @JsonProperty("bik")
    private String bik;

    @Pattern(regexp = "^\\d{20}$", message = "Расчетный счет должен содержать 20 цифр")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @Size(max = 255, message = "Название банка не должно превышать 255 символов")
    @JsonProperty("bankName")
    private String bankName;

    @Pattern(regexp = "^\\d{9}$", message = "КПП должен содержать 9 цифр")
    @JsonProperty("kpp")
    private String kpp;

    @Pattern(regexp = "^\\d{20}$", message = "Корреспондентский счет должен содержать 20 цифр")
    @JsonProperty("correspondentAccount")
    private String correspondentAccount;

    @JsonProperty("paymentMethod")
    private String paymentMethod;


    public boolean isIndividual() {
        return snils != null && !snils.isEmpty() ||
                phone != null && !phone.isEmpty() ||
                passportSeries != null && !passportSeries.isEmpty();
    }


    public boolean isLegalEntity() {
        return !isIndividual() && (bik != null && !bik.isEmpty() ||
                accountNumber != null && !accountNumber.isEmpty());
    }
}