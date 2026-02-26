package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PriceResponse {
    private BigDecimal basePrice;
    private BigDecimal additionalServicesPrice;
    private BigDecimal totalPrice;
    private Integer distanceKm;
    private Double carbonFootprintKg;
    private List<AdditionalServiceDto> recommendedServices;
    private String currency;

    @Data
    @Builder
    public static class AdditionalServiceDto {
        private String name;
        private String description;
        private BigDecimal price;
        private String reason;
    }
}