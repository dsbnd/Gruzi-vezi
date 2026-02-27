package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.Payment;
import com.rzd.dispatcher.model.entity.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Базовые методы
    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    // Новые методы для корпоративных платежей
    List<Payment> findByInn(String inn);

    List<Payment> findByInnAndStatus(String inn, PaymentStatus status);

    Optional<Payment> findByInnAndAmountAndStatus(String inn, BigDecimal amount, PaymentStatus status);

    Optional<Payment> findByPaymentDocument(String paymentDocument);

    List<Payment> findByBik(String bik);

    List<Payment> findByAccountNumber(String accountNumber);

    List<Payment> findByCompanyNameContainingIgnoreCase(String companyName);

    // Поиск с несколькими параметрами
    @Query("SELECT p FROM Payment p WHERE " +
            "(:inn IS NULL OR p.inn = :inn) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:companyName IS NULL OR LOWER(p.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))")
    List<Payment> searchPayments(@Param("inn") String inn,
                                 @Param("status") PaymentStatus status,
                                 @Param("companyName") String companyName);

    // Статистика по компании
    @Query("SELECT COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.inn = :inn AND p.status = 'SUCCEEDED'")
    Object[] getPaymentStatisticsByInn(@Param("inn") String inn);

    // Проверка на дубликат
    boolean existsByInnAndAmountAndPaymentPurposeAndStatusIn(
            String inn, BigDecimal amount, String paymentPurpose, List<PaymentStatus> statuses);

    // Просроченные платежи
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < CURRENT_DATE - 3")
    List<Payment> findOverduePayments();
}