package com.rzd.dispatcher.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "cargo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cargo {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    private Order order;

    @Column(name = "cargo_type", nullable = false, length = 255)
    private String cargoType;

    @Column(name = "weight_kg", nullable = false)
    private Integer weightKg;

    @Column(name = "volume_m3", nullable = false)
    private Integer volumeM3;

    @Column(name = "packaging_type", nullable = false, length = 100)
    private String packagingType;
}