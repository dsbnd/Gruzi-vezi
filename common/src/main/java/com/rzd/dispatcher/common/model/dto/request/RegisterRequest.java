package com.rzd.dispatcher.common.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "User type is required")
    @JsonProperty("userType")
    private String userType;


    private String companyName;
    private String inn;


    private String lastName;
    private String firstName;
    private String patronymic;
    private String phone;
    private String passportSeries;
    private String passportNumber;
    private String passportIssuedBy;
    private LocalDate passportIssuedDate;
    private String registrationAddress;
    private String snils;


    public void validate() {
        if ("LEGAL_ENTITY".equals(userType)) {
            validateLegalEntity();
        } else if ("INDIVIDUAL".equals(userType)) {
            validateIndividual();
        } else {
            throw new IllegalArgumentException("Invalid user type: " + userType);
        }
    }

    private void validateLegalEntity() {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Название компании обязательно для юридического лица");
        }
        if (inn == null || inn.isBlank()) {
            throw new IllegalArgumentException("ИНН обязателен для юридического лица");
        }
        String innStr = inn.trim();
        if (innStr.length() != 10 && innStr.length() != 12) {
            throw new IllegalArgumentException("ИНН должен содержать 10 или 12 цифр");
        }

        if (!innStr.matches("\\d+")) {
            throw new IllegalArgumentException("ИНН должен содержать только цифры");
        }
    }

    private void validateIndividual() {

        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Фамилия обязательна");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Имя обязательно");
        }


        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Номер телефона обязателен");
        }
        String phoneStr = phone.trim();
        if (!phoneStr.matches("^\\+?[0-9\\s\\-()]{10,20}$")) {
            throw new IllegalArgumentException("Неверный формат телефона");
        }


        if (inn == null || inn.isBlank()) {
            throw new IllegalArgumentException("ИНН обязателен для физического лица");
        }
        String innStr = inn.trim();
        if (innStr.length() != 12) {
            throw new IllegalArgumentException("ИНН физического лица должен содержать 12 цифр");
        }
        if (!innStr.matches("\\d+")) {
            throw new IllegalArgumentException("ИНН должен содержать только цифры");
        }


        if (snils == null || snils.isBlank()) {
            throw new IllegalArgumentException("СНИЛС обязателен");
        }
        String snilsStr = snils.trim();
        if (snilsStr.length() != 11) {
            throw new IllegalArgumentException("СНИЛС должен содержать 11 цифр");
        }
        if (!snilsStr.matches("\\d+")) {
            throw new IllegalArgumentException("СНИЛС должен содержать только цифры");
        }


        if (passportSeries == null || passportSeries.isBlank()) {
            throw new IllegalArgumentException("Серия паспорта обязательна");
        }
        String seriesStr = passportSeries.trim();
        if (seriesStr.length() != 4) {
            throw new IllegalArgumentException("Серия паспорта должна содержать 4 цифры");
        }
        if (!seriesStr.matches("\\d+")) {
            throw new IllegalArgumentException("Серия паспорта должна содержать только цифры");
        }

        if (passportNumber == null || passportNumber.isBlank()) {
            throw new IllegalArgumentException("Номер паспорта обязателен");
        }
        String numberStr = passportNumber.trim();
        if (numberStr.length() != 6) {
            throw new IllegalArgumentException("Номер паспорта должен содержать 6 цифр");
        }
        if (!numberStr.matches("\\d+")) {
            throw new IllegalArgumentException("Номер паспорта должен содержать только цифры");
        }

        if (passportIssuedBy == null || passportIssuedBy.isBlank()) {
            throw new IllegalArgumentException("Кем выдан паспорт - обязательно");
        }

        if (passportIssuedDate == null) {
            throw new IllegalArgumentException("Дата выдачи паспорта обязательна");
        }

        if (registrationAddress == null || registrationAddress.isBlank()) {
            throw new IllegalArgumentException("Адрес регистрации обязателен");
        }
    }
}