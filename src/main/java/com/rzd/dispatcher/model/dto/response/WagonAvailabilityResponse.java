package com.rzd.dispatcher.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WagonAvailabilityResponse {

    // Основная информация о вагоне
    private UUID wagonId;                 // ID вагона
    private String wagonNumber;            // Номер вагона
    private String wagonType;              // Тип вагона (крытый, полувагон и т.д.)

    // Технические характеристики
    private Integer maxWeightKg;           // Макс. грузоподъемность (кг)
    private Integer maxVolumeM3;           // Макс. объем (м³)
    private String currentStation;         // Текущая станция

    // Информация о доступности
    private Boolean isAvailable;           // Доступен ли
    private String availabilityStatus;      // Статус доступности (свободен, занят и т.д.)

    // Если вагон не на станции погрузки
    private Integer distanceToStation;      // Расстояние до станции погрузки (км)
    private Integer estimatedArrivalHours;  // Примерное время подачи (часов)

    // Рейтинг соответствия
    private Integer matchPercentage;        // Процент соответствия (0-100)
    private String recommendation;          // Рекомендация (ИДЕАЛЬНО, ХОРОШО и т.д.)

    // Цена
    private BigDecimal estimatedPrice;      // Примерная цена
    private String priceUnit;               // Единица цены (RUB)

    // Дополнительная информация
    private String ownerInfo;               // Информация о владельце
    private String lastMaintenanceDate;     // Дата последнего ТО
    private String maintenanceStatus;       // Статус ТО
}