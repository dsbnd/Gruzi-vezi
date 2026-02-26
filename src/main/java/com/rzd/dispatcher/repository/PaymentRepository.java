package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, Payment.PaymentStatus status);
}