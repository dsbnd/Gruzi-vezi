package com.rzd.dispatcher.model.dto.response;

import com.rzd.dispatcher.model.entity.Wagon;
import com.rzd.dispatcher.model.enums.WagonType;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class AvailableWagonResponse {
    private UUID wagonId;
    private String wagonNumber;
    private WagonType wagonType;
    private Integer maxWeightKg;
    private Integer maxVolumeM3;
    private String currentStation;
    private Double matchPercentage;
    private String recommendation;

    public static AvailableWagonResponse fromWagon(Wagon wagon,
                                                   Integer requiredWeight,
                                                   Integer requiredVolume) {
        double weightMatch = (double) requiredWeight / wagon.getMaxWeightKg() * 100;
        double volumeMatch = (double) requiredVolume / wagon.getMaxVolumeM3() * 100;
        double matchPercentage = Math.min(weightMatch, volumeMatch);

        String recommendation;
        if (matchPercentage > 90) {
            recommendation = "Идеально подходит";
        } else if (matchPercentage > 70) {
            recommendation = "Хорошо подходит";
        } else if (matchPercentage > 50) {
            recommendation = "Подходит с ограничениями";
        } else {
            recommendation = "Не рекомендуется";
        }

        return AvailableWagonResponse.builder()
                .wagonId(wagon.getId())
                .wagonNumber(wagon.getWagonNumber())
                .wagonType(wagon.getWagonType())
                .maxWeightKg(wagon.getMaxWeightKg())
                .maxVolumeM3(wagon.getMaxVolumeM3())
                .currentStation(wagon.getCurrentStation())
                .matchPercentage(Math.min(100, matchPercentage))
                .recommendation(recommendation)
                .build();
    }
}