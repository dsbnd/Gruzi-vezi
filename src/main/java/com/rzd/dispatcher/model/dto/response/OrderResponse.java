package com.rzd.dispatcher.model.dto.response;


import com.rzd.dispatcher.model.entity.Order;
import lombok.Builder;
import lombok.Data;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.model.enums.ServiceName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private String companyName;
    private String departureStation;
    private String destinationStation;
    private UUID wagonId;
    private String wagonNumber;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private BigDecimal carbonFootprintKg;
    private OffsetDateTime createdAt;
    private CargoDto cargo;
    private List<ServiceDto> services;

    @Data
    @Builder
    public static class CargoDto {
        private String cargoType;
        private Integer weightKg;
        private Integer volumeM3;
        private String packagingType;
    }

    @Data
    @Builder
    public static class ServiceDto {
        private ServiceName serviceName;
        private BigDecimal price;
    }

    public static OrderResponse fromOrder(Order order) {
        OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .companyName(order.getUser().getCompanyName())
                .departureStation(order.getDepartureStation())
                .destinationStation(order.getDestinationStation())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .carbonFootprintKg(order.getCarbonFootprintKg())
                .createdAt(order.getCreatedAt());

        if (order.getWagon() != null) {
            builder.wagonId(order.getWagon().getId())
                    .wagonNumber(order.getWagon().getWagonNumber());
        }

        if (order.getCargo() != null) {
            builder.cargo(CargoDto.builder()
                    .cargoType(order.getCargo().getCargoType())
                    .weightKg(order.getCargo().getWeightKg())
                    .volumeM3(order.getCargo().getVolumeM3())
                    .packagingType(order.getCargo().getPackagingType())
                    .build());
        }

        if (order.getServices() != null && !order.getServices().isEmpty()) {
            builder.services(order.getServices().stream()
                    .map(s -> ServiceDto.builder()
                            .serviceName(s.getServiceName())
                            .price(s.getPrice())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}