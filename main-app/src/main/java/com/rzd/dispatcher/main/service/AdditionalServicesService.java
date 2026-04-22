package com.rzd.dispatcher.main.service;

import com.rzd.dispatcher.common.model.dto.response.PriceResponse.AdditionalServiceDto;
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


    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.02");
    private static final BigDecimal MIN_INSURANCE = new BigDecimal("3000.00");
    private static final BigDecimal BASE_CARGO_VALUE_PER_TON = new BigDecimal("100000");


    private static final BigDecimal ELECTRONICS_COEFF = new BigDecimal("2.0");
    private static final BigDecimal DANGEROUS_COEFF = new BigDecimal("1.5");
    private static final BigDecimal PERISHABLE_COEFF = new BigDecimal("1.3");
    private static final BigDecimal MACHINERY_COEFF = new BigDecimal("1.8");
    private static final BigDecimal METAL_COEFF = new BigDecimal("0.8");


    private static final BigDecimal ESCORT_FIXED_PRICE = new BigDecimal("15000.00");
    private static final int ESCORT_WEIGHT_THRESHOLD = 50000;


    private static final BigDecimal EXPRESS_RATE = new BigDecimal("0.30");


    private static final BigDecimal TERMINAL_RATE_PER_TON = new BigDecimal("500");


    private static final BigDecimal GPS_RATE_PER_KM = new BigDecimal("10");
    private static final BigDecimal MIN_GPS_PRICE = new BigDecimal("2000.00");


    private static final BigDecimal CUSTOMS_FIXED_PRICE = new BigDecimal("25000.00");


    private static final BigDecimal LIABILITY_INSURANCE_RATE = new BigDecimal("0.01");

    public BigDecimal calculateInsurancePrice(String cargoType, Integer weightKg) {

        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);


        BigDecimal coefficient = getCargoCoefficient(cargoType);


        BigDecimal cargoValue = weightTons
                .multiply(BASE_CARGO_VALUE_PER_TON)
                .multiply(coefficient)
                .setScale(2, RoundingMode.HALF_UP);


        BigDecimal insurancePrice = cargoValue
                .multiply(INSURANCE_RATE)
                .setScale(2, RoundingMode.HALF_UP);


        if (insurancePrice.compareTo(MIN_INSURANCE) < 0) {
            insurancePrice = MIN_INSURANCE;
        }

        log.info("Страхование: вес={}кг, коэф={}, стоимость груза={} руб, страхование={} руб",
                weightKg, coefficient, cargoValue, insurancePrice);

        return insurancePrice;
    }

    public BigDecimal calculateLiabilityInsurance(BigDecimal freightPrice) {
        return freightPrice
                .multiply(LIABILITY_INSURANCE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

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
            return new BigDecimal("0.7");
        } else {
            return BigDecimal.ONE;
        }
    }

    private boolean isValuableCargo(String cargoType) {
        if (cargoType == null) return false;
        String type = cargoType.toLowerCase();
        return type.contains("электроник") ||
                type.contains("оборуд") ||
                type.contains("прибор") ||
                type.contains("запчаст") ||
                type.contains("machinery");
    }


    private boolean isDangerousCargo(String cargoType) {
        if (cargoType == null) return false;
        String type = cargoType.toLowerCase();
        return type.contains("хим") ||
                type.contains("нефть") ||
                type.contains("газ") ||
                type.contains("кислот") ||
                type.contains("взрыв");
    }

    private boolean isUrgentRoute(String from, String to) {
        if (from == null || to == null) return false;


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


        BigDecimal insurancePrice = calculateInsurancePrice(cargoType, weightKg);
        BigDecimal cargoValue = estimateCargoValue(cargoType, weightKg);
        services.add(AdditionalServiceDto.builder()
                .name("Страхование груза")
                .code("INSURANCE")
                .description(String.format(
                        "Полное страхование груза (2%% от стоимости). Оценочная стоимость: %s руб",
                        formatPrice(cargoValue)))
                .price(insurancePrice)
                .details(String.format("Ставка: 2%% | Мин. сумма: %s руб", formatPrice(MIN_INSURANCE)))
                .category("SAFETY")
                .build());


        BigDecimal liabilityPrice = calculateLiabilityInsurance(basePrice);
        services.add(AdditionalServiceDto.builder()
                .name("Страхование ответственности")
                .code("LIABILITY_INSURANCE")
                .description("Страхование гражданской ответственности перевозчика")
                .price(liabilityPrice)
                .details("1% от стоимости перевозки")
                .category("SAFETY")

                .build());


        services.add(AdditionalServiceDto.builder()
                .name("Вооруженное сопровождение")
                .code("ESCORT")
                .description("Квалифицированная охрана на всем маршруте следования")
                .price(ESCORT_FIXED_PRICE)
                .details("Фиксированная цена за маршрут")
                .category("SAFETY")

                .build());


        BigDecimal expressPrice = basePrice
                .multiply(EXPRESS_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        services.add(AdditionalServiceDto.builder()
                .name("Ускоренная доставка")
                .code("EXPRESS")
                .description("Сокращение сроков доставки на 30% (приоритетная обработка)")
                .price(expressPrice)
                .details("+30% к стоимости перевозки")
                .category("LOGISTICS")

                .build());


        BigDecimal weightTons = new BigDecimal(weightKg)
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
        BigDecimal terminalPrice = weightTons
                .multiply(TERMINAL_RATE_PER_TON)
                .setScale(2, RoundingMode.HALF_UP);
        services.add(AdditionalServiceDto.builder()
                .name("Терминальная обработка")
                .code("TERMINAL")
                .description("Погрузочно-разгрузочные работы на станциях отправления и назначения")
                .price(terminalPrice)
                .details(String.format("%s руб/тонна", formatPrice(TERMINAL_RATE_PER_TON)))
                .category("LOGISTICS")

                .build());


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



        boolean isInternational = isInternationalRoute(departureStation, destinationStation);
        if (isInternational) {
            services.add(AdditionalServiceDto.builder()
                    .name("Таможенное оформление")
                    .code("CUSTOMS")
                    .description("Полное таможенное сопровождение, подготовка деклараций")
                    .price(CUSTOMS_FIXED_PRICE)
                    .details("Фиксированная цена, включая консультацию")
                    .category("DOCUMENTS")

                    .build());
        }


        services.add(AdditionalServiceDto.builder()
                .name("Консолидация груза")
                .code("CONSOLIDATION")
                .description("Объединение с другими грузами для экономии (сборная перевозка)")
                .price(new BigDecimal("0"))
                .details("Экономия до 40% при загрузке < 50%")
                .category("LOGISTICS")

                .build());

        return services;
    }


    private boolean isInternationalRoute(String from, String to) {
        if (from == null || to == null) return false;


        List<String> borderStations = List.of(
                "Брест", "Гродно", "Смоленск", "Уссурийск", "Забайкальск",
                "Наушки", "Хасан", "Выборг", "Мурманск"
        );

        return borderStations.stream().anyMatch(station ->
                from.contains(station) || to.contains(station)
        );
    }

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
                return true;

            default:
                return false;
        }
    }

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


            if ("CONSOLIDATION".equals(service.getCode())) {
                isSelected = true;
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

                    break;
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%,.0f", price).replace(',', ' ');
    }
}