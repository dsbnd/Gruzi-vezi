package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String companyName;
    private String inn;
    private String role;
}