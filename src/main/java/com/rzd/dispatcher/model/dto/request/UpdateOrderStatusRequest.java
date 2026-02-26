package com.rzd.dispatcher.model.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.rzd.dispatcher.model.enums.OrderStatus;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}