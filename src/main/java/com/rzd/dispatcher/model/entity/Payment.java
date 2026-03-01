package com.rzd.dispatcher.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payment_id", unique = true)
    private String paymentId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod;

    // КОРПОРАТИВНЫЕ РЕКВИЗИТЫ
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "inn", length = 12)
    private String inn;

    @Column(name = "kpp", length = 9)
    private String kpp;

    @Column(name = "bik", length = 9)
    private String bik;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "correspondent_account", length = 20)
    private String correspondentAccount;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "payment_purpose")
    private String paymentPurpose;

    @Column(name = "payment_document", length = 50)
    private String paymentDocument;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    // ДОБАВЛЯЕМ поле metadata
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // Для хранения доп. информации о транзакции

    // Системные поля
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum PaymentStatus {
        PENDING,       // Ожидает оплаты
        PROCESSING,    // Обрабатывается банком
        SUCCEEDED,     // Оплачено успешно
        FAILED,        // Ошибка оплаты
        REFUNDED,      // Возврат
        WAITING_ACCEPT // Ожидает акцепта (для счетов)
    }
}