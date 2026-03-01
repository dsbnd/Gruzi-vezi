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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, UUID> {

    // Все счета компании по ИНН
    List<CompanyAccount> findAllByInnOrderByIsMainDescCreatedAtDesc(String inn);

    // Основной счет компании
    Optional<CompanyAccount> findByInnAndIsMainTrue(String inn);

    // Конкретный счет по номеру
    Optional<CompanyAccount> findByAccountNumber(String accountNumber);

    // Счет РЖД (должен быть один)
    Optional<CompanyAccount> findByIsRzdAccountTrue();

    // Проверка существования счета
    boolean existsByAccountNumber(String accountNumber);

    // Для блокировки при транзакциях
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CompanyAccount a WHERE a.accountNumber = :accountNumber")
    Optional<CompanyAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    // Списание средств
    @Modifying
    @Query("UPDATE CompanyAccount a SET a.balance = a.balance - :amount " +
            "WHERE a.accountNumber = :accountNumber AND a.balance >= :amount")
    int withdraw(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    // Зачисление средств
    @Modifying
    @Query("UPDATE CompanyAccount a SET a.balance = a.balance + :amount " +
            "WHERE a.accountNumber = :accountNumber")
    int deposit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}