package com.rzd.dispatcher.model.dto.request;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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