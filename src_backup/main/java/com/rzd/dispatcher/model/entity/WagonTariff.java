package com.rzd.dispatcher.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wagon_tariffs")
@Data
public class WagonTariff {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "wagon_type", nullable = false)
    private String wagonType;

    @Column(name = "cargo_type", nullable = false)
    private String cargoType;

    @Column(name = "base_rate_per_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRatePerKm;

    @Column(name = "coefficient", precision = 5, scale = 2)
    private BigDecimal coefficient = BigDecimal.ONE;

    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "description")
    private String description;
}