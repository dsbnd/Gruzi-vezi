package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.WagonSearchRequest;
import com.rzd.dispatcher.model.dto.response.WagonAvailabilityResponse;
import com.rzd.dispatcher.model.entity.*;
import com.rzd.dispatcher.model.enums.WagonStatus;
import com.rzd.dispatcher.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WagonSearchService {

    private final WagonRepository wagonRepository;
    private final WagonScheduleRepository scheduleRepository;
    private final WagonTariffRepository tariffRepository;
    private final StationDistanceRepository distanceRepository;
    private final RedisTemplate<String, String> redisTemplate; // Добавляем Redis

    private static final String WAGON_RESERVATION_KEY = "wagon:reserved:";

    /**
     * ОСНОВНОЙ МЕТОД: Поиск доступных вагонов под заявку
     */
    @Transactional(readOnly = true)
    public List<WagonAvailabilityResponse> findAvailableWagons(WagonSearchRequest request) {
        log.info("Поиск вагонов: станция={}, вес={}кг, тип={}",
                request.getDepartureStation(), request.getWeightKg(), request.getPreferredWagonType());

        // 1. Сначала ищем свободные вагоны на станции
        List<Wagon> wagonsOnStation = wagonRepository.findAvailableWagons(
                request.getDepartureStation(),
                request.getWeightKg(),
                request.getVolumeM3() != null ? request.getVolumeM3() : 0
        );

        log.info("Найдено вагонов в БД до фильтрации: {}", wagonsOnStation.size());
        for (Wagon w : wagonsOnStation) {
            log.info("  - Вагон: {}, вес: {}кг, объем: {}м³",
                    w.getWagonNumber(), w.getMaxWeightKg(), w.getMaxVolumeM3());
        }

        List<WagonAvailabilityResponse> result = new ArrayList<>();

        // 2. Фильтруем по типу вагона и проверяем доступность по датам
        // 2. Фильтруем по типу вагона и проверяем доступность по датам
        for (Wagon wagon : wagonsOnStation) {
            log.info("Проверка вагона: {}", wagon.getWagonNumber());

            // Фильтр по типу вагона, если указан
            if (request.getPreferredWagonType() != null) {
                log.info("  Тип вагона: {}, ищем: {}",
                        wagon.getWagonType().name(), request.getPreferredWagonType());
                if (!wagon.getWagonType().name().equalsIgnoreCase(request.getPreferredWagonType())) {
                    log.info("  ❌ Не подходит по типу");
                    continue;
                }
            }

            // Проверка резервации в Redis
            if (isWagonReserved(wagon.getId())) {
                log.info("  ❌ Вагон зарезервирован в Redis");
                continue;
            }

            // Проверка доступности по датам
            OffsetDateTime requiredDate = convertToOffsetDateTime(request.getRequiredDepartureDate());
            log.info("  Проверка доступности на дату: {}", requiredDate);

            if (isWagonAvailableForDates(wagon, requiredDate)) {
                log.info("  ✅ Вагон доступен");
                WagonAvailabilityResponse response = buildWagonResponse(wagon, request);
                result.add(response);
            } else {
                log.info("  ❌ Вагон не доступен по датам (есть конфликты в расписании)");
            }
        }

        // 3. Если мало вагонов - ищем на соседних станциях
        if (request.isAllowAlternativeStations() && result.size() < 3) {
            List<Wagon> nearbyWagons = findWagonsOnNearbyStations(request);
            for (Wagon wagon : nearbyWagons) {
                if (result.size() >= 10) break;

                if (isWagonReserved(wagon.getId())) {
                    continue; // Пропускаем зарезервированные
                }

                WagonAvailabilityResponse response = buildWagonResponseWithDistance(wagon, request);
                result.add(response);
            }
        }

        // 4. Сортируем по проценту соответствия
        result.sort((a, b) -> b.getMatchPercentage().compareTo(a.getMatchPercentage()));

        log.info("Найдено {} доступных вагонов", result.size());
        return result;
    }

    /**
     * НОВЫЙ МЕТОД: Резервирование вагона
     */
    @Transactional
    public boolean reserveWagon(UUID wagonId, UUID orderId, int minutes) {
        log.info("Резервирование вагона {} для заказа {} на {} минут", wagonId, orderId, minutes);

        // 1. Проверяем в Redis, не зарезервирован ли уже
        String redisKey = WAGON_RESERVATION_KEY + wagonId;
        Boolean isReserved = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, orderId.toString(), minutes, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isReserved)) {
            log.warn("Вагон {} уже зарезервирован", wagonId);
            return false;
        }

        try {
            // 2. Обновляем статус в БД - ИСПОЛЬЗУЕМ ENUM!
            Wagon wagon = wagonRepository.findById(wagonId)
                    .orElseThrow(() -> new RuntimeException("Вагон не найден"));

            if (wagon.getStatus() != WagonStatus.свободен) {
                redisTemplate.delete(redisKey);
                log.warn("Вагон {} не свободен (статус: {})", wagonId, wagon.getStatus());
                return false;
            }

            // ИСПРАВЛЕНО: используем enum WagonStatus.забронирован вместо строки
            wagon.setStatus(WagonStatus.забронирован);
            wagonRepository.save(wagon);

            // 3. Создаем запись в расписании - тоже используем enum
            WagonSchedule schedule = new WagonSchedule();
            schedule.setWagon(wagon);
            schedule.setOrderId(orderId);
            schedule.setStatus("зарезервирован"); // это строка, тут нормально
            schedule.setDepartureStation("ожидает");
            schedule.setArrivalStation("ожидает");
            scheduleRepository.save(schedule);

            log.info("Вагон {} успешно зарезервирован для заказа {}", wagonId, orderId);
            return true;

        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            log.error("Ошибка при резервировании вагона: {}", e.getMessage());
            throw e;
        }
    }
    /**
     * НОВЫЙ МЕТОД: Освобождение вагона
     */
    @Transactional
    public void releaseWagon(UUID wagonId) {
        log.info("Освобождение вагона {}", wagonId);

        // 1. Удаляем из Redis
        String redisKey = WAGON_RESERVATION_KEY + wagonId;
        redisTemplate.delete(redisKey);

        // 2. Обновляем статус в БД
        Wagon wagon = wagonRepository.findById(wagonId)
                .orElseThrow(() -> new RuntimeException("Вагон не найден"));

        wagon.setStatus(WagonStatus.свободен);
        wagonRepository.save(wagon);

        // 3. Обновляем расписание
        List<WagonSchedule> schedules = scheduleRepository.findByWagonId(wagonId);
        for (WagonSchedule schedule : schedules) {
            if ("зарезервирован".equals(schedule.getStatus())) {
                schedule.setStatus("отменен");
                scheduleRepository.save(schedule);
                break;
            }
        }

        log.info("Вагон {} освобожден", wagonId);
    }

    /**
     * НОВЫЙ МЕТОД: Проверка, зарезервирован ли вагон
     */
    private boolean isWagonReserved(UUID wagonId) {
        String redisKey = WAGON_RESERVATION_KEY + wagonId;
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(hasKey);
    }


    private OffsetDateTime convertToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atOffset(ZoneOffset.ofHours(3));
    }

    private boolean isWagonAvailableForDates(Wagon wagon, OffsetDateTime requiredDate) {
        if (requiredDate == null) return true;

        OffsetDateTime start = requiredDate.minusDays(1);
        OffsetDateTime end = requiredDate.plusDays(1);

        List<WagonSchedule> conflicts = scheduleRepository.findConflictingSchedules(
                wagon.getId(), start, end);

        return conflicts.isEmpty();
    }

    private List<Wagon> findWagonsOnNearbyStations(WagonSearchRequest request) {
        return wagonRepository.findAvailableWagons(
                        request.getDepartureStation(),
                        request.getWeightKg(),
                        request.getVolumeM3() != null ? request.getVolumeM3() : 0
                ).stream()
                .filter(w -> !w.getCurrentStation().equals(request.getDepartureStation()))
                .limit(20)
                .collect(Collectors.toList());
    }

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

    private WagonAvailabilityResponse buildWagonResponseWithDistance(Wagon wagon, WagonSearchRequest request) {
        int distance = getDistanceBetweenStations(
                wagon.getCurrentStation(), request.getDepartureStation());

        int matchPercentage = calculateMatchPercentage(wagon, request);
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
                .estimatedArrivalHours(distance / 50)
                .matchPercentage(matchPercentage)
                .recommendation(getRecommendation(matchPercentage))
                .estimatedPrice(estimatedPrice)
                .priceUnit("RUB")
                .build();
    }

    private int calculateMatchPercentage(Wagon wagon, WagonSearchRequest request) {
        int score = 100;

        double weightRatio = (double) request.getWeightKg() / wagon.getMaxWeightKg();
        if (weightRatio > 1.0) {
            return 0;
        } else if (weightRatio > 0.9) {
            score -= 0;
        } else if (weightRatio > 0.7) {
            score -= 5;
        } else if (weightRatio > 0.5) {
            score -= 15;
        } else {
            score -= 25;
        }

        if (request.getVolumeM3() != null && request.getVolumeM3() > 0) {
            double volumeRatio = (double) request.getVolumeM3() / wagon.getMaxVolumeM3();
            if (volumeRatio > 1.0) {
                return 0;
            } else if (volumeRatio < 0.3) {
                score -= 10;
            }
        }

        if (request.getPreferredWagonType() != null &&
                wagon.getWagonType().name().equalsIgnoreCase(request.getPreferredWagonType())) {
            score += 10;
        }

        return Math.min(100, Math.max(0, score));
    }

    private String getRecommendation(int percentage) {
        if (percentage >= 90) return "ИДЕАЛЬНО";
        if (percentage >= 75) return "ОТЛИЧНО";
        if (percentage >= 60) return "ХОРОШО";
        if (percentage >= 40) return "УДОВЛЕТВОРИТЕЛЬНО";
        return "НЕ РЕКОМЕНДУЕТСЯ";
    }

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

    private int getDistanceBetweenStations(String from, String to) {
        return distanceRepository.findByFromStationAndToStation(from, to)
                .map(StationDistance::getDistanceKm)
                .orElseGet(() -> distanceRepository.findByFromStationAndToStation(to, from)
                        .map(StationDistance::getDistanceKm)
                        .orElse(1000));
    }
}