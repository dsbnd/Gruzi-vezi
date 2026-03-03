package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.entity.CompanyAccount;
import com.rzd.dispatcher.repository.CompanyAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final CompanyAccountRepository accountRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public CompanyAccount createAccount(String inn, String companyName,
                                        String bik, String bankName,
                                        boolean isMain) {
        log.info("Создание счета для компании: {} (ИНН: {})", companyName, inn);


        String accountNumber = generateUniqueAccountNumber();

        CompanyAccount account = new CompanyAccount();
        account.setInn(inn);
        account.setCompanyName(companyName);
        account.setAccountNumber(accountNumber);
        account.setBalance(new BigDecimal("50000000.00"));
        account.setBik(bik);
        account.setBankName(bankName);
        account.setIsMain(isMain);
        account.setIsRzdAccount(false);


        if (isMain) {
            accountRepository.findAllByInnOrderByIsMainDescCreatedAtDesc(inn)
                    .forEach(a -> {
                        a.setIsMain(false);
                        accountRepository.save(a);
                    });
        }

        CompanyAccount savedAccount = accountRepository.save(account);
        log.info("Счет создан: {} для ИНН: {} с балансом {} руб",
                savedAccount.getAccountNumber(), inn, savedAccount.getBalance());

        return savedAccount;
    }


    @Transactional(readOnly = true)
    public List<CompanyAccount> getAccountsByInn(String inn) {
        return accountRepository.findAllByInnOrderByIsMainDescCreatedAtDesc(inn);
    }

    @Transactional(readOnly = true)
    public CompanyAccount getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Счет не найден: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(CompanyAccount::getBalance)
                .orElseThrow(() -> new RuntimeException("Счет не найден: " + accountNumber));
    }


@Transactional
public TransferResult transferMoney(String fromAccountNumber, String toAccountNumber,
                                    BigDecimal amount, String description) {
    log.info("Счет отправителя: {}", fromAccountNumber);
    log.info("Счет получателя: {}", toAccountNumber);
    log.info("Сумма перевода: {} руб", amount);

    CompanyAccount fromAccount = accountRepository.findByAccountNumberForUpdate(fromAccountNumber)
            .orElseThrow(() -> new RuntimeException("Счет отправителя не найден: " + fromAccountNumber));
    CompanyAccount toAccount = accountRepository.findByAccountNumberForUpdate(toAccountNumber)
            .orElseThrow(() -> new RuntimeException("Счет получателя не найден: " + toAccountNumber));

    BigDecimal beforeFrom = fromAccount.getBalance();
    BigDecimal beforeTo = toAccount.getBalance();

    log.info("До операции");
    log.info("Отправитель ({}): {} руб", fromAccount.getCompanyName(), beforeFrom);
    log.info("Получатель ({}): {} руб", toAccount.getCompanyName(), beforeTo);

    if (beforeFrom.compareTo(amount) < 0) {
        String errorMsg = String.format("Недостаточно средств. Доступно: %.2f руб", beforeFrom);
        log.error("Ошибка: {}", errorMsg);
        return TransferResult.failed(fromAccount, toAccount, amount, errorMsg, description);
    }

    int withdrawn = accountRepository.withdraw(fromAccountNumber, amount);
    if (withdrawn == 0) {
        String errorMsg = "Не удалось списать средства";
        log.error("Ошибка: {}", errorMsg);
        return TransferResult.failed(fromAccount, toAccount, amount, errorMsg, description);
    }

    int deposited = accountRepository.deposit(toAccountNumber, amount);
    if (deposited == 0) {
        accountRepository.deposit(fromAccountNumber, amount);
        throw new RuntimeException("Ошибка зачисления");
    }

    entityManager.flush();
    entityManager.clear();

    CompanyAccount updatedFromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
            .orElseThrow(() -> new RuntimeException("Счет отправителя не найден"));
    CompanyAccount updatedToAccount = accountRepository.findByAccountNumber(toAccountNumber)
            .orElseThrow(() -> new RuntimeException("Счет получателя не найден"));

    log.info("После операции");
    log.info("Отправитель ({}):", updatedFromAccount.getCompanyName());
    log.info("Было: {} руб", beforeFrom);
    log.info("Списано: {} руб", amount);
    log.info("Стало: {} руб ", updatedFromAccount.getBalance());
    log.info("Итог: {} руб ", updatedFromAccount.getBalance());
    log.info("");
    log.info("Получатель ({}):", updatedToAccount.getCompanyName());
    log.info("Было: {} руб", beforeTo);
    log.info("Зачислено: {} руб", amount);
    log.info("Стало: {} руб ", updatedToAccount.getBalance());
    log.info("Итог: {} руб ", updatedToAccount.getBalance());

    return TransferResult.success(updatedFromAccount, updatedToAccount, amount,
            beforeFrom, beforeTo, description);
}

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            String randomPart = String.format("%012d", (long)(Math.random() * 1000000000000L));
            accountNumber = "40702810" + randomPart;
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransferResult {
        private boolean success;
        private String message;
        private String fromAccountNumber;
        private String fromInn;
        private String fromName;
        private String toAccountNumber;
        private String toInn;
        private String toName;
        private BigDecimal fromBalanceBefore;
        private BigDecimal fromBalanceAfter;
        private BigDecimal toBalanceBefore;
        private BigDecimal toBalanceAfter;
        private BigDecimal amount;
        private String description;

        public static TransferResult success(CompanyAccount from, CompanyAccount to,
                                             BigDecimal amount,
                                             BigDecimal beforeFrom, BigDecimal beforeTo,
                                             String description) {
            return new TransferResult(true, "Перевод выполнен успешно",
                    from.getAccountNumber(), from.getInn(), from.getCompanyName(),
                    to.getAccountNumber(), to.getInn(), to.getCompanyName(),
                    beforeFrom, from.getBalance(),
                    beforeTo, to.getBalance(),
                    amount, description);
        }

        public static TransferResult failed(CompanyAccount from, CompanyAccount to,
                                            BigDecimal amount, String error,
                                            String description) {
            return new TransferResult(false, error,
                    from.getAccountNumber(), from.getInn(), from.getCompanyName(),
                    to.getAccountNumber(), to.getInn(), to.getCompanyName(),
                    from.getBalance(), from.getBalance(),
                    to.getBalance(), to.getBalance(),
                    amount, description);
        }

    }
}