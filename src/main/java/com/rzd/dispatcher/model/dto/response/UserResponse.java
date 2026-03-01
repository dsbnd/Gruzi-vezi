package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String email;
    private String companyName;
    private String inn;
}