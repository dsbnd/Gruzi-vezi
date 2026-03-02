package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.PriceCalculationRequest;
import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.model.entity.*;
import com.rzd.dispatcher.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
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
     * РАСЧЕТ ПОЛНОЙ СТОИМОСТИ С ВАГОНОМ
     */
    @Transactional(readOnly = true)
    public PriceResponse calculateFullPrice(UUID orderId, UUID wagonId, Set<String> selectedServices) {
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

        // 4. Получаем тип груза
        final String cargoTypeName;
        final Integer weightKg;
        if (order.getCargo() != null) {
            cargoTypeName = order.getCargo().getCargoType().name();
            weightKg = order.getCargo().getWeightKg();
        } else {
            cargoTypeName = "общий";
            weightKg = 0;
        }

        // 5. Получаем тариф
        final String wagonTypeName = wagon.getWagonType().name();

        WagonTariff tariff = wagonTariffRepository.findByWagonTypeAndCargoType(
                wagonTypeName,
                cargoTypeName
        ).orElseThrow(() -> new RuntimeException(
                "Тариф не найден для вагона: " + wagonTypeName + " и груза: " + cargoTypeName));

        // 6. Расчет базовой цены
        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal basePrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRatePerKm())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        if (tariff.getMinPrice() != null && basePrice.compareTo(tariff.getMinPrice()) < 0) {
            basePrice = tariff.getMinPrice();
        }

        log.info("Базовая цена: {} руб", basePrice);

        // 7. Получаем ВСЕ доступные услуги с флагами выбора
        List<PriceResponse.AdditionalServiceDto> allServices =
                additionalServicesService.getServicesWithSelection(
                        cargoTypeName,
                        order.getDepartureStation(),
                        order.getDestinationStation(),
                        weightKg,
                        basePrice,
                        distance,
                        selectedServices
                );

        // 8. Рассчитываем цену только ВЫБРАННЫХ услуг
        BigDecimal servicesPrice = additionalServicesService.calculateServicesPrice(
                selectedServices,
                cargoTypeName,
                weightKg,
                basePrice,
                distance,
                order.getDepartureStation(),
                order.getDestinationStation()
        );

        log.info("Цена выбранных услуг: {} руб", servicesPrice);

        // 9. Расчет углеродного следа
        double carbonFootprint = calculateCarbonFootprint(weightKg, distance);

        // 10. Оценка стоимости груза
        BigDecimal cargoValue = additionalServicesService.estimateCargoValue(cargoTypeName, weightKg);

        // 11. Формируем ответ
        PriceResponse response = PriceResponse.builder()
                .basePrice(basePrice)
                .additionalServicesPrice(servicesPrice)
                .totalPrice(basePrice.add(servicesPrice))
                .distanceKm(distance)
                .carbonFootprintKg(carbonFootprint)
                .availableServices(allServices)
                .currency("RUB")
                .cargoEstimate(PriceResponse.CargoEstimate.builder()
                        .estimatedValue(cargoValue)
                        .weightTons(weightTons)
                        .cargoType(cargoTypeName)
                        .riskLevel(determineRiskLevel(cargoTypeName, weightKg))
                        .build())
                .build();

        log.info("Расчет завершен. Итоговая цена: {} руб", response.getTotalPrice());
        return response;
    }

    /**
     * РАСЧЕТ СТОИМОСТИ ПО ЗАПРОСУ
     */
    @Transactional(readOnly = true)
    public PriceResponse calculatePrice(PriceCalculationRequest request) {
        log.info("Расчет стоимости по запросу: груз={}, вагон={}, вес={}кг",
                request.getCargoType(), request.getWagonType(), request.getWeightKg());

        int distance = getDistanceBetweenStations(
                request.getDepartureStation(),
                request.getDestinationStation()
        );

        WagonTariff tariff = wagonTariffRepository.findByWagonTypeAndCargoType(
                request.getWagonType(),
                request.getCargoType()
        ).orElseThrow(() -> new RuntimeException("Тариф не найден"));

        BigDecimal weightTons = new BigDecimal(request.getWeightKg())
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal basePrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRatePerKm())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        if (tariff.getMinPrice() != null && basePrice.compareTo(tariff.getMinPrice()) < 0) {
            basePrice = tariff.getMinPrice();
        }

        // Получаем все услуги с флагами выбора
        List<PriceResponse.AdditionalServiceDto> allServices =
                additionalServicesService.getServicesWithSelection(
                        request.getCargoType(),
                        request.getDepartureStation(),
                        request.getDestinationStation(),
                        request.getWeightKg(),
                        basePrice,
                        distance,
                        request.getSelectedServices()
                );

        // Рассчитываем цену выбранных услуг
        BigDecimal servicesPrice = additionalServicesService.calculateServicesPrice(
                request.getSelectedServices(),
                request.getCargoType(),
                request.getWeightKg(),
                basePrice,
                distance,
                request.getDepartureStation(),
                request.getDestinationStation()
        );

        double carbonFootprint = calculateCarbonFootprint(
                request.getWeightKg(), distance);

        BigDecimal cargoValue = additionalServicesService.estimateCargoValue(
                request.getCargoType(), request.getWeightKg());

        return PriceResponse.builder()
                .basePrice(basePrice)
                .additionalServicesPrice(servicesPrice)
                .totalPrice(basePrice.add(servicesPrice))
                .distanceKm(distance)
                .carbonFootprintKg(carbonFootprint)
                .availableServices(allServices)
                .currency("RUB")
                .cargoEstimate(PriceResponse.CargoEstimate.builder()
                        .estimatedValue(cargoValue)
                        .weightTons(weightTons)
                        .cargoType(request.getCargoType())
                        .riskLevel(determineRiskLevel(request.getCargoType(), request.getWeightKg()))
                        .build())
                .build();
    }

    /**
     * РАСЧЕТ ОРИЕНТИРОВОЧНОЙ ЦЕНЫ (БЕЗ ВАГОНА)
     * Используется для предварительной оценки
     */
    @Transactional(readOnly = true)
    public PriceResponse calculateEstimatedPrice(UUID orderId, String wagonType) {
        log.info("Расчет ориентировочной цены для заказа: {}, тип вагона: {}", orderId, wagonType);

        // 1. Получаем заказ
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден с ID: " + orderId));

        // 2. Получаем расстояние между станциями
        int distance = getDistanceBetweenStations(
                order.getDepartureStation(),
                order.getDestinationStation()
        );
        log.info("Расстояние между станциями: {} км", distance);

        // 3. Получаем тип груза
        final String cargoTypeName;
        final Integer weightKg;
        if (order.getCargo() != null) {
            cargoTypeName = order.getCargo().getCargoType().name();
            weightKg = order.getCargo().getWeightKg();
        } else {
            cargoTypeName = "общий";
            weightKg = 0;
        }

        // 4. Пытаемся найти тариф, если нет - используем средний
        WagonTariff tariff;
        try {
            tariff = wagonTariffRepository.findByWagonTypeAndCargoType(
                    wagonType,
                    cargoTypeName
            ).orElseThrow(() -> new RuntimeException("Тариф не найден"));
        } catch (Exception e) {
            // Если точный тариф не найден, создаем тариф по умолчанию
            log.warn("Тариф не найден для вагона {} и груза {}, используем значения по умолчанию",
                    wagonType, cargoTypeName);
            tariff = new WagonTariff();
            tariff.setBaseRatePerKm(new BigDecimal("12.00"));
            tariff.setCoefficient(BigDecimal.ONE);
            tariff.setMinPrice(new BigDecimal("4000.00"));
        }

        // 5. Расчет базовой цены
        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal estimatedPrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRatePerKm())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        // Проверка минимальной цены
        if (tariff.getMinPrice() != null && estimatedPrice.compareTo(tariff.getMinPrice()) < 0) {
            estimatedPrice = tariff.getMinPrice();
        }

        // 6. Расчет углеродного следа
        double carbonFootprint = calculateCarbonFootprint(weightKg, distance);

        // 7. Оценка стоимости груза
        BigDecimal cargoValue = additionalServicesService.estimateCargoValue(cargoTypeName, weightKg);

        log.info("Ориентировочная цена: {} руб", estimatedPrice);

        // 8. Формируем ответ (без дополнительных услуг, только оценка)
        return PriceResponse.builder()
                .basePrice(estimatedPrice)
                .additionalServicesPrice(BigDecimal.ZERO)
                .totalPrice(estimatedPrice)
                .distanceKm(distance)
                .carbonFootprintKg(carbonFootprint)
                .currency("RUB")
                .cargoEstimate(PriceResponse.CargoEstimate.builder()
                        .estimatedValue(cargoValue)
                        .weightTons(weightTons)
                        .cargoType(cargoTypeName)
                        .riskLevel(determineRiskLevel(cargoTypeName, weightKg))
                        .build())
                .build();
    }

    /**
     * ОПРЕДЕЛЕНИЕ УРОВНЯ РИСКА
     */
    private String determineRiskLevel(String cargoType, Integer weightKg) {
        if (cargoType == null) return "Средний";

        String type = cargoType.toLowerCase();

        if (type.contains("хим") || type.contains("нефть") || type.contains("взрыв")) {
            return "Высокий";
        } else if (type.contains("электроник") || type.contains("оборуд")) {
            return "Средний";
        } else if (weightKg > 50000) {
            return "Повышенный";
        } else {
            return "Низкий";
        }
    }

    /**
     * ПОЛУЧЕНИЕ РАССТОЯНИЯ МЕЖДУ СТАНЦИЯМИ
     */
    private int getDistanceBetweenStations(String from, String to) {
        return distanceRepository.findByFromStationAndToStation(from, to)
                .map(StationDistance::getDistanceKm)
                .orElseGet(() -> distanceRepository.findByFromStationAndToStation(to, from)
                        .map(StationDistance::getDistanceKm)
                        .orElse(1000));
    }

    /**
     * РАСЧЕТ УГЛЕРОДНОГО СЛЕДА
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