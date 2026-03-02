package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.model.dto.response.PriceResponse.AdditionalServiceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class AdditionalServicesService {

    // === КОНСТАНТЫ ДЛЯ РАСЧЕТОВ ===

    // Страхование
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.02"); // 2% от стоимости груза
    private static final BigDecimal MIN_INSURANCE = new BigDecimal("3000.00"); // Мин. страхование
    private static final BigDecimal BASE_CARGO_VALUE_PER_TON = new BigDecimal("100000"); // 100 000 руб/тонна

    // Коэффициенты для разных типов грузов
    private static final BigDecimal ELECTRONICS_COEFF = new BigDecimal("2.0");  // электроника дороже
    private static final BigDecimal DANGEROUS_COEFF = new BigDecimal("1.5");    // опасные грузы
    private static final BigDecimal PERISHABLE_COEFF = new BigDecimal("1.3");   // скоропортящиеся
    private static final BigDecimal MACHINERY_COEFF = new BigDecimal("1.8");     // оборудование
    private static final BigDecimal METAL_COEFF = new BigDecimal("0.8");         // металл дешевле

    // Сопровождение
    private static final BigDecimal ESCORT_FIXED_PRICE = new BigDecimal("15000.00");
    private static final int ESCORT_WEIGHT_THRESHOLD = 50000; // 50 тонн

    // Ускоренная доставка
    private static final BigDecimal EXPRESS_RATE = new BigDecimal("0.30"); // 30% от стоимости

    // Терминальная обработка
    private static final BigDecimal TERMINAL_RATE_PER_TON = new BigDecimal("500"); // 500 руб/тонна

    // GPS-мониторинг
    private static final BigDecimal GPS_RATE_PER_KM = new BigDecimal("10"); // 10 руб/км
    private static final BigDecimal MIN_GPS_PRICE = new BigDecimal("2000.00");

    // Таможенное оформление
    private static final BigDecimal CUSTOMS_FIXED_PRICE = new BigDecimal("25000.00");

    // Страхование ответственности
    private static final BigDecimal LIABILITY_INSURANCE_RATE = new BigDecimal("0.01"); // 1%

    /**
     * РАСЧЕТ СТОИМОСТИ СТРАХОВАНИЯ ГРУЗА
     * Формула: Вес(тонны) * Базовая стоимость тонны * Коэф. типа груза * 2%
     */
    public BigDecimal calculateInsurancePrice(String cargoType, Integer weightKg) {
        // 1. Переводим вес в тонны
        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        // 2. Получаем коэффициент типа груза
        BigDecimal coefficient = getCargoCoefficient(cargoType);

        // 3. Расчет оценочной стоимости груза
        BigDecimal cargoValue = weightTons
                .multiply(BASE_CARGO_VALUE_PER_TON)
                .multiply(coefficient)
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Расчет страховой премии (2%)
        BigDecimal insurancePrice = cargoValue
                .multiply(INSURANCE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Проверка минимальной суммы
        if (insurancePrice.compareTo(MIN_INSURANCE) < 0) {
            insurancePrice = MIN_INSURANCE;
        }

        log.info("Страхование: вес={}кг, коэф={}, стоимость груза={} руб, страхование={} руб",
                weightKg, coefficient, cargoValue, insurancePrice);

        return insurancePrice;
    }

    /**
     * РАСЧЕТ СТОИМОСТИ СТРАХОВАНИЯ ОТВЕТСТВЕННОСТИ
     * 1% от стоимости перевозки
     */
    public BigDecimal calculateLiabilityInsurance(BigDecimal freightPrice) {
        return freightPrice
                .multiply(LIABILITY_INSURANCE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ПОЛУЧЕНИЕ КОЭФФИЦИЕНТА ТИПА ГРУЗА
     */
    private BigDecimal getCargoCoefficient(String cargoType) {
        if (cargoType == null) return BigDecimal.ONE;

        String type = cargoType.toLowerCase();

        if (type.contains("электроник") || type.contains("computer")) {
            return ELECTRONICS_COEFF;
        } else if (type.contains("оборуд") || type.contains("станок") || type.contains("machinery")) {
            return MACHINERY_COEFF;
        } else if (type.contains("хим") || type.contains("нефть") || type.contains("кислот")) {
            return DANGEROUS_COEFF;
        } else if (type.contains("зерно") || type.contains("продукт") || type.contains("еда")) {
            return PERISHABLE_COEFF;
        } else if (type.contains("метал") || type.contains("сталь") || type.contains("чугун")) {
            return METAL_COEFF;
        } else if (type.contains("уголь") || type.contains("руда")) {
            return new BigDecimal("0.7"); // Низкая стоимость
        } else {
            return BigDecimal.ONE;
        }
    }

    /**
     * ПРОВЕРКА, ЯВЛЯЕТСЯ ЛИ ГРУЗ ЦЕННЫМ
     */
    private boolean isValuableCargo(String cargoType) {
        if (cargoType == null) return false;
        String type = cargoType.toLowerCase();
        return type.contains("электроник") ||
                type.contains("оборуд") ||
                type.contains("прибор") ||
                type.contains("запчаст") ||
                type.contains("machinery");
    }

    /**
     * ПРОВЕРКА, ЯВЛЯЕТСЯ ЛИ ГРУЗ ОПАСНЫМ
     */
    private boolean isDangerousCargo(String cargoType) {
        if (cargoType == null) return false;
        String type = cargoType.toLowerCase();
        return type.contains("хим") ||
                type.contains("нефть") ||
                type.contains("газ") ||
                type.contains("кислот") ||
                type.contains("взрыв");
    }

    /**
     * ПРОВЕРКА, ЯВЛЯЕТСЯ ЛИ МАРШРУТ СРОЧНЫМ
     */
    private boolean isUrgentRoute(String from, String to) {
        if (from == null || to == null) return false;

        // Основные направления
        boolean isMoscow = from.contains("Москва") || to.contains("Москва");
        boolean isSpb = from.contains("Питер") || from.contains("Петербург") ||
                to.contains("Питер") || to.contains("Петербург");
        boolean isEkaterinburg = from.contains("Екатеринбург") || to.contains("Екатеринбург");
        boolean isNovosibirsk = from.contains("Новосибирск") || to.contains("Новосибирск");

        return (isMoscow && isSpb) ||
                (isMoscow && isEkaterinburg) ||
                (isMoscow && isNovosibirsk);
    }

    /**
     * ОЦЕНКА СТОИМОСТИ ГРУЗА (для отображения)
     */
    public BigDecimal estimateCargoValue(String cargoType, Integer weightKg) {
        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);

        BigDecimal coefficient = getCargoCoefficient(cargoType);

        return weightTons
                .multiply(BASE_CARGO_VALUE_PER_TON)
                .multiply(coefficient)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ПОЛУЧЕНИЕ ВСЕХ ДОСТУПНЫХ УСЛУГ С РАСЧЕТОМ ЦЕН
     */
    public List<AdditionalServiceDto> getAllAvailableServices(
            String cargoType,
            Integer weightKg,
            BigDecimal basePrice,
            Integer distanceKm,
            String departureStation,
            String destinationStation) {

        List<AdditionalServiceDto> services = new ArrayList<>();

        // 1. СТРАХОВАНИЕ ГРУЗА
        BigDecimal insurancePrice = calculateInsurancePrice(cargoType, weightKg);
        BigDecimal cargoValue = estimateCargoValue(cargoType, weightKg);
        services.add(AdditionalServiceDto.builder()
                .name("📋 Страхование груза")
                .code("INSURANCE")
                .description(String.format(
                        "Полное страхование груза (2%% от стоимости). Оценочная стоимость: %s руб",
                        formatPrice(cargoValue)))
                .price(insurancePrice)
                .details(String.format("Ставка: 2%% | Мин. сумма: %s руб", formatPrice(MIN_INSURANCE)))
                .category("SAFETY")
                .icon("🛡️")
                .build());

        // 2. СТРАХОВАНИЕ ОТВЕТСТВЕННОСТИ
        BigDecimal liabilityPrice = calculateLiabilityInsurance(basePrice);
        services.add(AdditionalServiceDto.builder()
                .name("📋 Страхование ответственности")
                .code("LIABILITY_INSURANCE")
                .description("Страхование гражданской ответственности перевозчика")
                .price(liabilityPrice)
                .details("1% от стоимости перевозки")
                .category("SAFETY")
                .icon("📄")
                .build());

        // 3. СОПРОВОЖДЕНИЕ (ОХРАНА)
        services.add(AdditionalServiceDto.builder()
                .name("🚔 Вооруженное сопровождение")
                .code("ESCORT")
                .description("Квалифицированная охрана на всем маршруте следования")
                .price(ESCORT_FIXED_PRICE)
                .details("Фиксированная цена за маршрут")
                .category("SAFETY")
                .icon("🔫")
                .build());

        // 4. УСКОРЕННАЯ ДОСТАВКА
        BigDecimal expressPrice = basePrice
                .multiply(EXPRESS_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        services.add(AdditionalServiceDto.builder()
                .name("⚡ Ускоренная доставка")
                .code("EXPRESS")
                .description("Сокращение сроков доставки на 30% (приоритетная обработка)")
                .price(expressPrice)
                .details("+30% к стоимости перевозки")
                .category("LOGISTICS")
                .icon("🚀")
                .build());

        // 5. ТЕРМИНАЛЬНАЯ ОБРАБОТКА
        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
        BigDecimal terminalPrice = weightTons
                .multiply(TERMINAL_RATE_PER_TON)
                .setScale(2, RoundingMode.HALF_UP);
        services.add(AdditionalServiceDto.builder()
                .name("🏗️ Терминальная обработка")
                .code("TERMINAL")
                .description("Погрузочно-разгрузочные работы на станциях отправления и назначения")
                .price(terminalPrice)
                .details(String.format("%s руб/тонна", formatPrice(TERMINAL_RATE_PER_TON)))
                .category("LOGISTICS")
                .icon("📦")
                .build());

        // 6. GPS-МОНИТОРИНГ
        BigDecimal gpsPrice;
        if (distanceKm != null && distanceKm > 0) {
            gpsPrice = new BigDecimal(distanceKm)
                    .multiply(GPS_RATE_PER_KM)
                    .setScale(2, RoundingMode.HALF_UP);
            if (gpsPrice.compareTo(MIN_GPS_PRICE) < 0) {
                gpsPrice = MIN_GPS_PRICE;
            }
        } else {
            gpsPrice = MIN_GPS_PRICE;
        }
        services.add(AdditionalServiceDto.builder()
                .name("🛰️ GPS-мониторинг")
                .code("TRACKING")
                .description("Отслеживание груза в реальном времени с уведомлениями в Telegram/SMS")
                .price(gpsPrice)
                .details(String.format("%s руб/км, мин. %s руб",
                        formatPrice(GPS_RATE_PER_KM), formatPrice(MIN_GPS_PRICE)))
                .category("MONITORING")
                .icon("📍")
                .build());

        // 7. ТАМОЖЕННОЕ ОФОРМЛЕНИЕ
        boolean isInternational = isInternationalRoute(departureStation, destinationStation);
        if (isInternational) {
            services.add(AdditionalServiceDto.builder()
                    .name("🛃 Таможенное оформление")
                    .code("CUSTOMS")
                    .description("Полное таможенное сопровождение, подготовка деклараций")
                    .price(CUSTOMS_FIXED_PRICE)
                    .details("Фиксированная цена, включая консультацию")
                    .category("DOCUMENTS")
                    .icon("📑")
                    .build());
        }

        // 8. КОНСОЛИДАЦИЯ ГРУЗА
        services.add(AdditionalServiceDto.builder()
                .name("📦 Консолидация груза")
                .code("CONSOLIDATION")
                .description("Объединение с другими грузами для экономии (сборная перевозка)")
                .price(new BigDecimal("0")) // Бесплатно, но влияет на сроки
                .details("Экономия до 40% при загрузке < 50%")
                .category("LOGISTICS")
                .icon("🔄")
                .build());

        return services;
    }

    /**
     * ПРОВЕРКА МЕЖДУНАРОДНОГО МАРШРУТА
     */
    private boolean isInternationalRoute(String from, String to) {
        if (from == null || to == null) return false;

        // Список погранпереходов
        List<String> borderStations = List.of(
                "Брест", "Гродно", "Смоленск", "Уссурийск", "Забайкальск",
                "Наушки", "Хасан", "Выборг", "Мурманск"
        );

        return borderStations.stream().anyMatch(station ->
                from.contains(station) || to.contains(station)
        );
    }

    /**
     * ПОЛУЧЕНИЕ РЕКОМЕНДОВАННЫХ УСЛУГ
     */
    public List<AdditionalServiceDto> getRecommendedServices(
            String cargoType,
            String departureStation,
            String destinationStation,
            Integer weightKg,
            BigDecimal basePrice,
            Integer distanceKm) {

        List<AdditionalServiceDto> recommendations = new ArrayList<>();
        List<AdditionalServiceDto> allServices = getAllAvailableServices(
                cargoType, weightKg, basePrice, distanceKm,
                departureStation, destinationStation);

        for (AdditionalServiceDto service : allServices) {
            if (isServiceRecommended(service, cargoType, weightKg, departureStation,
                    destinationStation, distanceKm)) {
                recommendations.add(service);
            }
        }

        return recommendations;
    }

    /**
     * ПРОВЕРКА, РЕКОМЕНДУЕТСЯ ЛИ УСЛУГА
     */
    private boolean isServiceRecommended(AdditionalServiceDto service,
                                         String cargoType,
                                         Integer weightKg,
                                         String departureStation,
                                         String destinationStation,
                                         Integer distanceKm) {

        switch (service.getCode()) {
            case "INSURANCE":
                return isValuableCargo(cargoType) || weightKg > 30000;

            case "LIABILITY_INSURANCE":
                return weightKg > 40000 || isDangerousCargo(cargoType);

            case "ESCORT":
                return weightKg > ESCORT_WEIGHT_THRESHOLD || isDangerousCargo(cargoType);

            case "EXPRESS":
                return isUrgentRoute(departureStation, destinationStation);

            case "TERMINAL":
                return weightKg > 20000;

            case "TRACKING":
                return distanceKm != null && distanceKm > 1000;

            case "CUSTOMS":
                return isInternationalRoute(departureStation, destinationStation);

            case "CONSOLIDATION":
                return true; // Всегда показываем как опцию

            default:
                return false;
        }
    }

    /**
     * ПОЛУЧЕНИЕ УСЛУГ С ФЛАГАМИ ВЫБОРА И РЕКОМЕНДАЦИЙ
     */
    public List<AdditionalServiceDto> getServicesWithSelection(
            String cargoType,
            String departureStation,
            String destinationStation,
            Integer weightKg,
            BigDecimal basePrice,
            Integer distanceKm,
            Set<String> selectedServiceCodes) {

        List<AdditionalServiceDto> allServices = getAllAvailableServices(
                cargoType, weightKg, basePrice, distanceKm,
                departureStation, destinationStation);

        List<AdditionalServiceDto> result = new ArrayList<>();

        for (AdditionalServiceDto service : allServices) {
            boolean isRecommended = isServiceRecommended(
                    service, cargoType, weightKg, departureStation,
                    destinationStation, distanceKm);

            boolean isSelected = selectedServiceCodes != null &&
                    selectedServiceCodes.contains(service.getCode());

            // Для консолидации особая логика - она бесплатная
            if ("CONSOLIDATION".equals(service.getCode())) {
                isSelected = true; // Всегда доступна
            }

            AdditionalServiceDto serviceWithFlags = AdditionalServiceDto.builder()
                    .name(service.getName())
                    .code(service.getCode())
                    .description(service.getDescription())
                    .price(service.getPrice())
                    .details(service.getDetails())
                    .category(service.getCategory())
                    .icon(service.getIcon())
                    .isRecommended(isRecommended)
                    .isSelected(isSelected)
                    .recommendationReason(getRecommendationReason(service, cargoType, weightKg, distanceKm))
                    .build();

            result.add(serviceWithFlags);
        }

        return result;
    }

    /**
     * ПОЛУЧЕНИЕ ТЕКСТА ПРИЧИНЫ РЕКОМЕНДАЦИИ
     */
    private String getRecommendationReason(AdditionalServiceDto service,
                                           String cargoType,
                                           Integer weightKg,
                                           Integer distanceKm) {
        switch (service.getCode()) {
            case "INSURANCE":
                if (isValuableCargo(cargoType)) {
                    return "Ценный груз требует страховки";
                } else if (weightKg > 30000) {
                    return "Крупная партия рекомендуется к страхованию";
                }
                return "Рекомендуется для вашего груза";

            case "ESCORT":
                if (weightKg > ESCORT_WEIGHT_THRESHOLD) {
                    return "Тяжеловесный груз требует сопровождения";
                } else if (isDangerousCargo(cargoType)) {
                    return "Опасный груз требует охраны";
                }
                return "Рекомендуется для безопасности";

            case "EXPRESS":
                return "Срочная доставка по данному маршруту";

            case "TRACKING":
                if (distanceKm != null && distanceKm > 2000) {
                    return "Для дальних перевозок рекомендуется мониторинг";
                }
                return "Контроль местоположения в реальном времени";

            case "TERMINAL":
                return "Услуги терминала для вашего груза";

            default:
                return null;
        }
    }

    /**
     * РАСЧЕТ СТОИМОСТИ ВЫБРАННЫХ УСЛУГ
     */
    public BigDecimal calculateServicesPrice(
            Set<String> selectedServiceCodes,
            String cargoType,
            Integer weightKg,
            BigDecimal basePrice,
            Integer distanceKm,
            String departureStation,
            String destinationStation) {

        if (selectedServiceCodes == null || selectedServiceCodes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;

        for (String code : selectedServiceCodes) {
            switch (code) {
                case "INSURANCE":
                    total = total.add(calculateInsurancePrice(cargoType, weightKg));
                    break;

                case "LIABILITY_INSURANCE":
                    total = total.add(calculateLiabilityInsurance(basePrice));
                    break;

                case "ESCORT":
                    total = total.add(ESCORT_FIXED_PRICE);
                    break;

                case "EXPRESS":
                    total = total.add(basePrice.multiply(EXPRESS_RATE));
                    break;

                case "TERMINAL":
                    BigDecimal weightTons = new BigDecimal(weightKg)
                            .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
                    total = total.add(weightTons.multiply(TERMINAL_RATE_PER_TON));
                    break;

                case "TRACKING":
                    if (distanceKm != null && distanceKm > 0) {
                        BigDecimal gpsPrice = new BigDecimal(distanceKm)
                                .multiply(GPS_RATE_PER_KM)
                                .setScale(2, RoundingMode.HALF_UP);
                        if (gpsPrice.compareTo(MIN_GPS_PRICE) < 0) {
                            gpsPrice = MIN_GPS_PRICE;
                        }
                        total = total.add(gpsPrice);
                    } else {
                        total = total.add(MIN_GPS_PRICE);
                    }
                    break;

                case "CUSTOMS":
                    if (isInternationalRoute(departureStation, destinationStation)) {
                        total = total.add(CUSTOMS_FIXED_PRICE);
                    }
                    break;

                case "CONSOLIDATION":
                    // Бесплатно, ничего не добавляем
                    break;
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ФОРМАТИРОВАНИЕ ЦЕНЫ ДЛЯ ОТОБРАЖЕНИЯ
     */
    private String formatPrice(BigDecimal price) {
        return String.format("%,.0f", price).replace(',', ' ');
    }
}