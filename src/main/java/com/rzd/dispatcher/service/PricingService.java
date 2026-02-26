package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.PriceCalculationRequest;
import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.model.entity.Tariff;
import com.rzd.dispatcher.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final TariffRepository tariffRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AdditionalServicesService additionalServicesService;

    private static final String DISTANCE_CACHE_KEY = "distance:";
    private static final BigDecimal CO2_FACTOR = new BigDecimal("0.02");

    @Transactional
    public PriceResponse calculatePrice(PriceCalculationRequest request) {
        log.info("Расчет стоимости: {}", request);

        // 1. Получаем расстояние (из Redis или рассчитываем)
        int distance = getDistance(request.getDepartureStation(), request.getDestinationStation());

        // 2. Получаем тариф из БД
        Tariff tariff = tariffRepository.findByCargoTypeAndWagonType(
                        request.getCargoType(), request.getWagonType())
                .orElseThrow(() -> new RuntimeException("Тариф не найден"));

        // 3. Базовая цена
        BigDecimal weightTons = new BigDecimal(request.getWeightKg())
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);

        BigDecimal basePrice = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.getBaseRate())
                .multiply(tariff.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Доп. услуги
        List<PriceResponse.AdditionalServiceDto> additionalServices =
                additionalServicesService.recommendServices(request);

        BigDecimal additionalPrice = additionalServices.stream()
                .map(PriceResponse.AdditionalServiceDto::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Углеродный след
        double carbonFootprint = calculateCarbonFootprint(request.getWeightKg(), distance);

        return PriceResponse.builder()
                .basePrice(basePrice)
                .additionalServicesPrice(additionalPrice)
                .totalPrice(basePrice.add(additionalPrice))
                .distanceKm(distance)
                .carbonFootprintKg(carbonFootprint)
                .recommendedServices(additionalServices)
                .currency("RUB")
                .build();
    }

    private int getDistance(String from, String to) {
        String cacheKey = DISTANCE_CACHE_KEY + from + ":" + to;

        // 1. Пробуем получить из Redis
        Integer cachedDistance = (Integer) redisTemplate.opsForValue().get(cacheKey);
        if (cachedDistance != null) {
            log.debug("Расстояние получено из Redis: {} км", cachedDistance);
            return cachedDistance;
        }

        // 2. Если нет в Redis - рассчитываем
        int distance = calculateDistance(from, to);

        // 3. Сохраняем в Redis на 24 часа
        redisTemplate.opsForValue().set(cacheKey, distance, 24, TimeUnit.HOURS);
        log.info("Расстояние сохранено в Redis: {} -> {} = {} км", from, to, distance);

        return distance;
    }

    private int calculateDistance(String from, String to) {
        // Эмуляция расчета расстояния
        if (from.contains("Москва") && to.contains("Питер")) return 650;
        if (from.contains("Москва") && to.contains("Екатеринбург")) return 1667;
        if (from.contains("Москва") && to.contains("Новосибирск")) return 2811;
        if (from.contains("Москва") && to.contains("Казань")) return 797;
        if (from.contains("Питер") && to.contains("Мурманск")) return 1240;
        return 1000;
    }

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