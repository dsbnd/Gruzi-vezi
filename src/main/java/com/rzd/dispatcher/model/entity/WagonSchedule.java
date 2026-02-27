package com.rzd.dispatcher.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wagon_schedule")
@Data
public class WagonSchedule {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "wagon_id", nullable = false)
    private Wagon wagon;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "departure_station", nullable = false)
    private String departureStation;

    @Column(name = "arrival_station", nullable = false)
    private String arrivalStation;

    @Column(name = "departure_date")
    private OffsetDateTime departureDate;

    @Column(name = "arrival_date")
    private OffsetDateTime arrivalDate;

    @Column(name = "status")
    private String status = "запланирован";

    @Column(name = "cargo_type")
    private String cargoType;

    @Column(name = "cargo_weight_kg")
    private Integer cargoWeightKg;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}