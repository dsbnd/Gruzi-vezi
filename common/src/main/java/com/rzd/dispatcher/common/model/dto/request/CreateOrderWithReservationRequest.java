package com.rzd.dispatcher.common.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateOrderWithReservationRequest {

    @Valid
    @NotNull(message = "Данные заявки обязательны")
    private CreateOrderRequest orderRequest;

    @NotNull(message = "ID вагона обязателен")
    private UUID wagonId;

    private Set<String> selectedServices;
}