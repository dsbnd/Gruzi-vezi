package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.WagonSearchRequest;
import com.rzd.dispatcher.model.dto.response.WagonAvailabilityResponse;
import com.rzd.dispatcher.model.entity.*;
import com.rzd.dispatcher.model.enums.WagonStatus;
import com.rzd.dispatcher.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WagonSearchService {

    private final WagonRepository wagonRepository;
    private final WagonScheduleRepository scheduleRepository;
    private final WagonTariffRepository tariffRepository;
    private final StationDistanceRepository distanceRepository;

    /**
     * ОСНОВНОЙ МЕТОД: Поиск доступных вагонов под заявку
     */
    @Transactional(readOnly = true)
    public List<WagonAvailabilityResponse> findAvailableWagons(WagonSearchRequest request) {
        log.info("🔍 ПОИСК ВАГОНОВ: станция={}, вес={}кг, тип={}",
                request.getDepartureStation(), request.getWeightKg(), request.getPreferredWagonType());

        // 1. Сначала ищем свободные вагоны на станции через findAvailableWagons (уже есть в репозитории)
        List<Wagon> wagonsOnStation = wagonRepository.findAvailableWagons(
                request.getDepartureStation(),
                request.getWeightKg(),
                request.getVolumeM3() != null ? request.getVolumeM3() : 0
        );

        List<WagonAvailabilityResponse> result = new ArrayList<>();

        // 2. Фильтруем по типу вагона и проверяем доступность по датам
        for (Wagon wagon : wagonsOnStation) {
            // Фильтр по типу вагона, если указан
            if (request.getPreferredWagonType() != null &&
                    !wagon.getWagonType().name().equalsIgnoreCase(request.getPreferredWagonType())) {
                continue;
            }

            // Проверяем, свободен ли вагон в нужные даты (конвертируем LocalDateTime в OffsetDateTime)
            if (isWagonAvailableForDates(wagon, convertToOffsetDateTime(request.getRequiredDepartureDate()))) {
                WagonAvailabilityResponse response = buildWagonResponse(wagon, request);
                result.add(response);
            }
        }

        // 3. Если мало вагонов - ищем на соседних станциях
        if (request.isAllowAlternativeStations() && result.size() < 3) {
            List<Wagon> nearbyWagons = findWagonsOnNearbyStations(request);
            for (Wagon wagon : nearbyWagons) {
                if (result.size() >= 10) break;

                WagonAvailabilityResponse response = buildWagonResponseWithDistance(wagon, request);
                result.add(response);
            }
        }

        // 4. Сортируем по проценту соответствия
        result.sort((a, b) -> b.getMatchPercentage().compareTo(a.getMatchPercentage()));

        log.info("✅ Найдено {} доступных вагонов", result.size());
        return result;
    }

    /**
     * Конвертер LocalDateTime в OffsetDateTime
     */
    private OffsetDateTime convertToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atOffset(ZoneOffset.ofHours(3)); // MSK timezone
    }

    /**
     * Проверка доступности вагона на дату
     */
    private boolean isWagonAvailableForDates(Wagon wagon, OffsetDateTime requiredDate) {
        if (requiredDate == null) return true;

        OffsetDateTime start = requiredDate.minusDays(1);
        OffsetDateTime end = requiredDate.plusDays(1);

        List<WagonSchedule> conflicts = scheduleRepository.findConflictingSchedules(
                wagon.getId(), start, end);

        return conflicts.isEmpty();
    }

    /**
     * Поиск на соседних станциях
     */
    private List<Wagon> findWagonsOnNearbyStations(WagonSearchRequest request) {
        // Ищем все свободные вагоны с нужной грузоподъемностью
        return wagonRepository.findAvailableWagons(
                        request.getDepartureStation(),
                        request.getWeightKg(),
                        request.getVolumeM3() != null ? request.getVolumeM3() : 0
                ).stream()
                .filter(w -> !w.getCurrentStation().equals(request.getDepartureStation())) // исключаем те, что уже на станции
                .limit(20)
                .collect(Collectors.toList());
    }

    /**
     * Формирование ответа для вагона
     */
    private WagonAvailabilityResponse buildWagonResponse(Wagon wagon, WagonSearchRequest request) {
        int matchPercentage = calculateMatchPercentage(wagon, request);
        BigDecimal estimatedPrice = calculateEstimatedPrice(wagon, request);

        return WagonAvailabilityResponse.builder()
                .wagonId(wagon.getId())
                .wagonNumber(wagon.getWagonNumber())
                .wagonType(wagon.getWagonType().name())
                .maxWeightKg(wagon.getMaxWeightKg())
                .maxVolumeM3(wagon.getMaxVolumeM3())
                .currentStation(wagon.getCurrentStation())
                .isAvailable(true)
                .availabilityStatus(wagon.getStatus().name())
                .distanceToStation(0)
                .estimatedArrivalHours(0)
                .matchPercentage(matchPercentage)
                .recommendation(getRecommendation(matchPercentage))
                .estimatedPrice(estimatedPrice)
                .priceUnit("RUB")
                .build();
    }

    /**
     * Формирование ответа для вагона с учетом расстояния
     */
    private WagonAvailabilityResponse buildWagonResponseWithDistance(Wagon wagon, WagonSearchRequest request) {
        int distance = getDistanceBetweenStations(
                wagon.getCurrentStation(), request.getDepartureStation());

        int matchPercentage = calculateMatchPercentage(wagon, request);
        // Штраф за расстояние (чем дальше, тем меньше процент)
        int distancePenalty = Math.min(30, distance / 10);
        matchPercentage = Math.max(0, matchPercentage - distancePenalty);

        BigDecimal estimatedPrice = calculateEstimatedPrice(wagon, request);

        return WagonAvailabilityResponse.builder()
                .wagonId(wagon.getId())
                .wagonNumber(wagon.getWagonNumber())
                .wagonType(wagon.getWagonType().name())
                .maxWeightKg(wagon.getMaxWeightKg())
                .maxVolumeM3(wagon.getMaxVolumeM3())
                .currentStation(wagon.getCurrentStation())
                .isAvailable(true)
                .availabilityStatus(wagon.getStatus().name())
                .distanceToStation(distance)
                .estimatedArrivalHours(distance / 50) // 50 км/ч средняя скорость подачи
                .matchPercentage(matchPercentage)
                .recommendation(getRecommendation(matchPercentage))
                .estimatedPrice(estimatedPrice)
                .priceUnit("RUB")
                .build();
    }

    /**
     * Расчет процента соответствия
     */
    private int calculateMatchPercentage(Wagon wagon, WagonSearchRequest request) {
        int score = 100;

        // Оценка по весу
        double weightRatio = (double) request.getWeightKg() / wagon.getMaxWeightKg();
        if (weightRatio > 1.0) {
            return 0; // вагон не подходит по весу
        } else if (weightRatio > 0.9) {
            score -= 0; // отлично, почти полная загрузка
        } else if (weightRatio > 0.7) {
            score -= 5; // хорошая загрузка
        } else if (weightRatio > 0.5) {
            score -= 15; // средняя загрузка
        } else {
            score -= 25; // очень маленький груз для такого вагона
        }

        // Оценка по объему (если указан)
        if (request.getVolumeM3() != null && request.getVolumeM3() > 0) {
            double volumeRatio = (double) request.getVolumeM3() / wagon.getMaxVolumeM3();
            if (volumeRatio > 1.0) {
                return 0; // вагон не подходит по объему
            } else if (volumeRatio < 0.3) {
                score -= 10; // очень маленький объем
            }
        }

        // Бонус за точное совпадение типа вагона
        if (request.getPreferredWagonType() != null &&
                wagon.getWagonType().name().equalsIgnoreCase(request.getPreferredWagonType())) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Получение рекомендации
     */
    private String getRecommendation(int percentage) {
        if (percentage >= 90) return "ИДЕАЛЬНО";
        if (percentage >= 75) return "ОТЛИЧНО";
        if (percentage >= 60) return "ХОРОШО";
        if (percentage >= 40) return "УДОВЛЕТВОРИТЕЛЬНО";
        return "НЕ РЕКОМЕНДУЕТСЯ";
    }

    /**
     * Расчет примерной цены
     */
    private BigDecimal calculateEstimatedPrice(Wagon wagon, WagonSearchRequest request) {
        int distance = getDistanceBetweenStations(
                request.getDepartureStation(), request.getArrivalStation());

        Optional<WagonTariff> tariff = tariffRepository.findByWagonTypeAndCargoType(
                wagon.getWagonType().name(),
                request.getCargoType() != null ? request.getCargoType() : "общий"
        );

        if (tariff.isEmpty()) return BigDecimal.ZERO;

        BigDecimal weightTons = new BigDecimal(request.getWeightKg())
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal price = weightTons
                .multiply(new BigDecimal(distance))
                .multiply(tariff.get().getBaseRatePerKm())
                .multiply(tariff.get().getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        if (tariff.get().getMinPrice() != null &&
                price.compareTo(tariff.get().getMinPrice()) < 0) {
            price = tariff.get().getMinPrice();
        }

        return price;
    }

    /**
     * Получение расстояния между станциями
     */
    private int getDistanceBetweenStations(String from, String to) {
        return distanceRepository.findByFromStationAndToStation(from, to)
                .map(StationDistance::getDistanceKm)
                .orElseGet(() -> {
                    // Пробуем обратное направление
                    return distanceRepository.findByFromStationAndToStation(to, from)
                            .map(StationDistance::getDistanceKm)
                            .orElse(1000); // значение по умолчанию
                });
    }
}