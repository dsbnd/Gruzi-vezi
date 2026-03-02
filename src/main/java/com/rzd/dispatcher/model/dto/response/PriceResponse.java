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
    private List<AdditionalServiceDto> availableServices;
    private List<AdditionalServiceDto> recommendedServices;
    private String currency;
    private CargoEstimate cargoEstimate;

    @Data
    @Builder
    public static class AdditionalServiceDto {
        private String name;
        private String code;
        private String description;
        private String details;
        private String category; // SAFETY, LOGISTICS, DOCUMENTS, MONITORING
        private String icon;
        private BigDecimal price;
        private String recommendationReason;
        private Boolean isRecommended;
        private Boolean isSelected;
    }

    @Data
    @Builder
    public static class CargoEstimate {
        private BigDecimal estimatedValue;
        private BigDecimal weightTons;
        private String cargoType;
        private String riskLevel;
    }
}