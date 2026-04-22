package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String email;
    private String userType;
    private String displayName;

    private String role;

    // Для юрлиц
    private String companyName;
    private String inn;

    // Для физлиц
    private String firstName;
    private String lastName;
    private String patronymic;
    private String phone;
}