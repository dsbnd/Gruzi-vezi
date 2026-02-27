package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.CompanyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, UUID> {

    Optional<CompanyAccount> findByInn(String inn);

    Optional<CompanyAccount> findByAccountNumber(String accountNumber);

    // Пессимистичная блокировка для безопасности при списании
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CompanyAccount a WHERE a.inn = :inn")
    Optional<CompanyAccount> findByInnForUpdate(@Param("inn") String inn);

    // Списание средств (проверка что достаточно средств)
    @Modifying
    @Query("UPDATE CompanyAccount a SET a.balance = a.balance - :amount " +
            "WHERE a.inn = :inn AND a.balance >= :amount")
    int withdraw(@Param("inn") String inn, @Param("amount") BigDecimal amount);

    // Зачисление средств
    @Modifying
    @Query("UPDATE CompanyAccount a SET a.balance = a.balance + :amount " +
            "WHERE a.inn = :inn")
    int deposit(@Param("inn") String inn, @Param("amount") BigDecimal amount);
}