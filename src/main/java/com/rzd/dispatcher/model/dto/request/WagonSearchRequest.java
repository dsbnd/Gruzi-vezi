package com.rzd.dispatcher.model.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WagonSearchRequest {

    @NotNull(message = "ID заказа обязателен")
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