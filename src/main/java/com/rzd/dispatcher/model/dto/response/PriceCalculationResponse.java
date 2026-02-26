package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PriceCalculationResponse {
    private BigDecimal basePrice;
    private BigDecimal additionalServicesPrice;
    private BigDecimal totalPrice;
    private Integer distanceKm;
    private Double carbonFootprintKg;
    private List<AdditionalServiceOffer> additionalServices;
    private String tariffDescription;

    @Data
    @Builder
    public static class AdditionalServiceOffer {
        private String serviceName;
        private BigDecimal price;
        private String description;
        private String recommendationReason;
    }
}