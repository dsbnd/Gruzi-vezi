package com.rzd.dispatcher.common.repository;

import com.rzd.dispatcher.model.entity.CompanyAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, UUID> {

    
    List<CompanyAccount> findAllByInnOrderByIsMainDescCreatedAtDesc(String inn);

    
    Optional<CompanyAccount> findByInnAndIsMainTrue(String inn);

    
    Optional<CompanyAccount> findByAccountNumber(String accountNumber);

    
    Optional<CompanyAccount> findByIsRzdAccountTrue();

    
    boolean existsByAccountNumber(String accountNumber);

    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CompanyAccount a WHERE a.accountNumber = :accountNumber")
    Optional<CompanyAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    
    @Modifying
    @Query("UPDATE CompanyAccount a SET a.balance = a.balance - :amount " +
            "WHERE a.accountNumber = :accountNumber AND a.balance >= :amount")
    int withdraw(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    
    @Modifying
    @Query("UPDATE CompanyAccount a SET a.balance = a.balance + :amount " +
            "WHERE a.accountNumber = :accountNumber")
    int deposit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}