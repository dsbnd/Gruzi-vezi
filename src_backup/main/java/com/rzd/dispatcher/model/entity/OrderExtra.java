package com.rzd.dispatcher.model.entity;

import com.rzd.dispatcher.model.enums.ServiceName;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderExtra {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_name", nullable = false)
    private ServiceName serviceName;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}