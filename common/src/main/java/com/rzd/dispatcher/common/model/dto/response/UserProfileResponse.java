package com.rzd.dispatcher.common.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String userType;
    // Для юрлиц
    private String companyName;
    private String inn;

    // Для физлиц
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

    private String role;
    private String displayName;
}