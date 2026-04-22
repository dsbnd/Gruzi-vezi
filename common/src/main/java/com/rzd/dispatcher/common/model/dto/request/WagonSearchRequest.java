package com.rzd.dispatcher.common.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WagonSearchRequest {

    private UUID orderId;

    @NotBlank(message = "Станция отправления обязательна")
    private String departureStation;

    @NotBlank(message = "Станция назначения обязательна")
    private String arrivalStation;

    @NotNull(message = "Вес груза обязателен")
    @Min(1)
    private Integer weightKg;

    private Integer volumeM3;

    private String cargoType;

    private String preferredWagonType;

    private LocalDateTime requiredDepartureDate;

    private boolean allowAlternativeStations = true;
}