package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.PriceCalculationRequest;
import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.model.entity.*;
import com.rzd.dispatcher.model.enums.CargoType;
import com.rzd.dispatcher.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final OrderRepository orderRepository;
    private final WagonRepository wagonRepository;
    private final WagonTariffRepository wagonTariffRepository;
    private final StationDistanceRepository distanceRepository;
    private final AdditionalServicesService additionalServicesService;

    private static final BigDecimal CO2_FACTOR = new BigDecimal("0.02");

    /**
     * Расчет полной стоимости с вагоном (использует enum)
     */
    @Transactional(readOnly = true)
    public PriceResponse calculateFullPrice(UUID orderId, UUID wagonId) {
        log.info("Расчет полной стоимости для заказа: {}, вагон: {}", orderId, wagonId);

        // 1. Получаем заказ
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден с ID: " + orderId));

        // 2. Получаем вагон
        Wagon wagon = wagonRepository.findById(wagonId)
                .orElseThrow(() -> new RuntimeException("Вагон не найден с ID: " + wagonId));

        // 3. Получаем расстояние между станциями
        int distance = getDistanceBetweenStations(
                order.getDepartureStation(),
                order.getDestinationStation()
        );
        log.info("Расстояние между станциями: {} км", distance);

        // 4. Получаем тип груза из enum - делаем FINAL переменную
        final String cargoTypeName;  // объявляем как final
        if (order.getCargo() != null && order.getCargo().getCargoType() != null) {
            cargoTypeName = order.getCargo().getCargoType().name();
            log.info("Тип груза (enum): {}, (string): {}",
                    order.getCargo().getCargoType(), cargoTypeName);
        } else {
            cargoTypeName = "общий";
        }

        // 5. Получаем тариф - ИСПРАВЛЕНО: используем final переменную
        final String wagonTypeName = wagon.getWagonType().name(); // тоже делаем final

        WagonTariff tariff = wagonTariffRepository.findByWagonTypeAndCargoType(
                wagonTypeName,  // используем final переменную
                cargoTypeName   // используем final переменную
        ).orElseThrow(() -> new RuntimeException(
                "Тариф не найден для вагона: " + wagonTypeName +  // используем переменные
                        " и груза: " + cargoTypeName));

        // 6. Расчет базовой цены
        BigDecimal weightTons = new BigDecimal(order.getCargo().getWeightKg())
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal basePrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRatePerKm())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        // Проверка минимальной цены
        if (tariff.getMinPrice() != null &&
                basePrice.compareTo(tariff.getMinPrice()) < 0) {
            basePrice = tariff.getMinPrice();
        }

        log.info("Базовая цена: {} руб", basePrice);

        // 7. Получаем рекомендованные дополнительные услуги
        final String cargoTypeForServices = cargoTypeName; // еще одна final переменная
        List<PriceResponse.AdditionalServiceDto> services =
                additionalServicesService.recommendServices(
                        cargoTypeForServices,
                        order.getDepartureStation(),
                        order.getDestinationStation(),
                        order.getCargo().getWeightKg()
                );

        BigDecimal servicesPrice = services.stream()
                .map(PriceResponse.AdditionalServiceDto::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Цена дополнительных услуг: {} руб", servicesPrice);

        // 8. Расчет углеродного следа
        double carbonFootprint = calculateCarbonFootprint(
                order.getCargo().getWeightKg(), distance);

        // 9. Формируем ответ
        PriceResponse response = PriceResponse.builder()
                .basePrice(basePrice)
                .additionalServicesPrice(servicesPrice)
                .totalPrice(basePrice.add(servicesPrice))
                .distanceKm(distance)
                .carbonFootprintKg(carbonFootprint)
                .recommendedServices(services)
                .currency("RUB")
                .build();

        log.info("Расчет завершен. Итоговая цена: {} руб", response.getTotalPrice());
        return response;
    }
    /**
     * Расчет стоимости по запросу (для контроллера)
     */
    @Transactional(readOnly = true)
    public PriceResponse calculatePrice(PriceCalculationRequest request) {
        log.info("Расчет стоимости по запросу: груз={}, вагон={}, вес={}кг, маршрут={}->{}",
                request.getCargoType(), request.getWagonType(), request.getWeightKg(),
                request.getDepartureStation(), request.getDestinationStation());

        // 1. Получаем расстояние между станциями
        int distance = getDistanceBetweenStations(
                request.getDepartureStation(),
                request.getDestinationStation()
        );
        log.info("Расстояние между станциями: {} км", distance);

        // 2. Получаем тариф для типа вагона и типа груза (все строки)
        WagonTariff tariff = wagonTariffRepository.findByWagonTypeAndCargoType(
                request.getWagonType(),
                request.getCargoType()
        ).orElseThrow(() -> new RuntimeException(
                "Тариф не найден для вагона: " + request.getWagonType() +
                        " и груза: " + request.getCargoType()));

        // 3. Расчет базовой цены
        BigDecimal weightTons = new BigDecimal(request.getWeightKg())
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal basePrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRatePerKm())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        // Проверка минимальной цены
        if (tariff.getMinPrice() != null &&
                basePrice.compareTo(tariff.getMinPrice()) < 0) {
            basePrice = tariff.getMinPrice();
        }

        log.info("Базовая цена: {} руб", basePrice);

        // 4. Получаем рекомендованные дополнительные услуги
        List<PriceResponse.AdditionalServiceDto> services =
                additionalServicesService.recommendServices(
                        request.getCargoType(),
                        request.getDepartureStation(),
                        request.getDestinationStation(),
                        request.getWeightKg()
                );

        BigDecimal servicesPrice = services.stream()
                .map(PriceResponse.AdditionalServiceDto::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Цена дополнительных услуг: {} руб", servicesPrice);

        // 5. Расчет углеродного следа
        double carbonFootprint = calculateCarbonFootprint(
                request.getWeightKg(), distance);

        // 6. Формируем ответ
        PriceResponse response = PriceResponse.builder()
                .basePrice(basePrice)
                .additionalServicesPrice(servicesPrice)
                .totalPrice(basePrice.add(servicesPrice))
                .distanceKm(distance)
                .carbonFootprintKg(carbonFootprint)
                .recommendedServices(services)
                .currency("RUB")
                .build();

        log.info("Расчет завершен. Итоговая цена: {} руб", response.getTotalPrice());
        return response;
    }

    /**
     * Расчет ориентировочной цены без вагона
     */
    @Transactional(readOnly = true)
    public PriceResponse calculateEstimatedPrice(UUID orderId, String wagonType) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        int distance = getDistanceBetweenStations(
                order.getDepartureStation(),
                order.getDestinationStation()
        );

        // Получаем тип груза из enum
        String cargoTypeName = order.getCargo() != null && order.getCargo().getCargoType() != null ?
                order.getCargo().getCargoType().name() : "общий";

        // Используем средний тариф для типа вагона
        WagonTariff tariff = wagonTariffRepository.findByWagonTypeAndCargoType(
                wagonType,
                cargoTypeName
        ).orElseThrow(() -> new RuntimeException("Тариф не найден"));

        BigDecimal weightTons = new BigDecimal(order.getCargo().getWeightKg())
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal estimatedPrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRatePerKm())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        return PriceResponse.builder()
                .basePrice(estimatedPrice)
                .totalPrice(estimatedPrice)
                .distanceKm(distance)
                .carbonFootprintKg(calculateCarbonFootprint(
                        order.getCargo().getWeightKg(), distance))
                .currency("RUB")
                .build();
    }

    /**
     * Получение расстояния между станциями
     */
    private int getDistanceBetweenStations(String from, String to) {
        return distanceRepository.findByFromStationAndToStation(from, to)
                .map(StationDistance::getDistanceKm)
                .orElseGet(() -> distanceRepository.findByFromStationAndToStation(to, from)
                        .map(StationDistance::getDistanceKm)
                        .orElse(1000));
    }

    /**
     * Расчет углеродного следа
     */
    private double calculateCarbonFootprint(Integer weightKg, Integer distanceKm) {
        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);

        return weightTons
                .multiply(new BigDecimal(distanceKm))
                .multiply(CO2_FACTOR)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}