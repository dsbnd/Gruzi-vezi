package com.rzd.dispatcher.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Departure station is required")
    @Size(max = 255, message = "Departure station must be less than 255 characters")
    private String departureStation;

    @NotBlank(message = "Destination station is required")
    @Size(max = 255, message = "Destination station must be less than 255 characters")
    private String destinationStation;

    @NotNull(message = "Cargo information is required")
    private CargoDto cargo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoDto {

        @NotBlank(message = "Cargo type is required")
        private String cargoType;

        @NotNull(message = "Weight is required")
        @Min(value = 1, message = "Weight must be at least 1 kg")
        @Max(value = 1000000, message = "Weight must be less than 1000000 kg")
        private Integer weightKg;

        @NotNull(message = "Volume is required")
        @Min(value = 1, message = "Volume must be at least 1 m3")
        private Integer volumeM3;

        @NotBlank(message = "Packaging type is required")
        private String packagingType;
    }
}