package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private String inn;
    private String companyName;
    private String accountNumber;
    private BigDecimal balance;
    private String bik;
    private String bankName;
    private Boolean isMain;
    private Boolean isRzdAccount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}