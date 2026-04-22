package com.rzd.dispatcher.common.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"inn", "account_number"}))
@Data
public class CompanyAccount {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false, length = 12)
    private String inn;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 9)
    private String bik;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "is_main")
    private Boolean isMain = false;

    @Column(name = "is_rzd_account")
    private Boolean isRzdAccount = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}