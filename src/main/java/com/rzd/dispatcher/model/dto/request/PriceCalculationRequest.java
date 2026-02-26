package com.rzd.dispatcher.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PriceCalculationRequest {

    @NotBlank(message = "Тип груза обязателен")
    private String cargoType;

    @NotBlank(message = "Тип вагона обязателен")
    private String wagonType;

    @NotNull(message = "Вес груза обязателен")
    @Min(value = 1, message = "Вес должен быть больше 0")
    private Integer weightKg;

    @NotBlank(message = "Станция отправления обязательна")
    private String departureStation;

    @NotBlank(message = "Станция назначения обязательна")
    private String destinationStation;
}