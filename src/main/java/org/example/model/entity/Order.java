package org.example.model.entity;

import org.example.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "departure_station", nullable = false, length = 255)
    private String departureStation;

    @Column(name = "destination_station", nullable = false, length = 255)
    private String destinationStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wagon_id")
    private Wagon wagon;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.черновик;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "carbon_footprint_kg", precision = 10, scale = 2)
    private BigDecimal carbonFootprintKg;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cargo cargo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderExtra> services = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }


    public void addService(OrderExtra service) {
        services.add(service);
        service.setOrder(this);
    }

    public void removeService(OrderExtra service) {
        services.remove(service);
        service.setOrder(null);
    }
}