package com.rzd.dispatcher.model.dto.response;

import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.enums.CargoType;
import com.rzd.dispatcher.model.enums.OrderStatus;
import com.rzd.dispatcher.model.enums.PackagingType;
import com.rzd.dispatcher.model.enums.ServiceName;
import com.rzd.dispatcher.model.enums.WagonType;
import lombok.Builder;
import lombok.Data;

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

    // Добавили желаемый тип вагона, чтобы фронтенд его видел
    private WagonType requestedWagonType;

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
        private CargoType cargoType;         // Изменили String на Enum
        private Integer weightKg;
        private Integer volumeM3;
        private PackagingType packagingType; // Изменили String на Enum
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
                .requestedWagonType(order.getRequestedWagonType()) // Прокинули это поле из сущности
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .carbonFootprintKg(order.getCarbonFootprintKg())
                .createdAt(order.getCreatedAt());

        if (order.getWagon() != null) {
            builder.wagonId(order.getWagon().getId())
                    .wagonNumber(order.getWagon().getWagonNumber());
        }

        if (order.getCargo() != null) {
            // Теперь здесь идеальное совпадение типов (Enum -> Enum)
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