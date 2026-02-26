package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.PriceCalculationRequest;
import com.rzd.dispatcher.model.dto.response.PriceResponse.AdditionalServiceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AdditionalServicesService {

    public List<AdditionalServiceDto> recommendServices(PriceCalculationRequest request) {
        List<AdditionalServiceDto> recommendations = new ArrayList<>();

        // Страхование для ценных грузов
        if (isValuableCargo(request.getCargoType())) {
            recommendations.add(AdditionalServiceDto.builder()
                    .name("Страхование груза")
                    .description("Полное страхование груза от повреждений и утери")
                    .price(new BigDecimal("5000.00"))
                    .reason("Рекомендуется для ценных грузов")
                    .build());
        }

        // Сопровождение для тяжелых грузов
        if (request.getWeightKg() > 50000) {
            recommendations.add(AdditionalServiceDto.builder()
                    .name("Сопровождение")
                    .description("Сопровождение груза экспедитором")
                    .price(new BigDecimal("15000.00"))
                    .reason("Тяжеловесный груз требует сопровождения")
                    .build());
        }

        // Ускоренная доставка для срочных маршрутов
        if (isUrgentRoute(request.getDepartureStation(), request.getDestinationStation())) {
            recommendations.add(AdditionalServiceDto.builder()
                    .name("Ускоренная доставка")
                    .description("Приоритетная обработка и сокращение сроков")
                    .price(new BigDecimal("12000.00"))
                    .reason("Для срочных перевозок")
                    .build());
        }

        return recommendations;
    }

    private boolean isValuableCargo(String cargoType) {
        return cargoType.equalsIgnoreCase("Электроника") ||
                cargoType.equalsIgnoreCase("Оборудование") ||
                cargoType.equalsIgnoreCase("Запчасти");
    }

    private boolean isUrgentRoute(String from, String to) {
        return (from.contains("Москва") && to.contains("Питер")) ||
                (from.contains("Москва") && to.contains("Екатеринбург"));
    }
}