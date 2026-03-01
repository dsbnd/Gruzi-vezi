package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TransferResponse {
    private boolean success;
    private String message;
    private String fromAccountNumber;
    private String fromInn;
    private String fromName;
    private String toAccountNumber;
    private String toInn;
    private String toName;
    private BigDecimal fromBalanceBefore;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceBefore;
    private BigDecimal toBalanceAfter;
    private BigDecimal amount;
    private String description;
}