package org.example.model.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.model.enums.WagonStatus;
import org.example.model.enums.WagonType;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "wagons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wagon {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "wagon_number", unique = true, nullable = false, length = 50)
    private String wagonNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "wagon_type", nullable = false)
    private WagonType wagonType;

    @Column(name = "max_weight_kg", nullable = false)
    private Integer maxWeightKg;

    @Column(name = "max_volume_m3", nullable = false)
    private Integer maxVolumeM3;

    @Column(name = "current_station", nullable = false, length = 255)
    private String currentStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WagonStatus status = WagonStatus.свободен;
}