package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.entity.CompanyAccount;
import com.rzd.dispatcher.repository.CompanyAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final CompanyAccountRepository accountRepository;

    // ИНН РЖД (константа)
    private static final String RZD_INN = "7708503727";

    /**
     * Создание счета при регистрации компании
     */
    @Transactional
    public CompanyAccount createAccountForCompany(String inn, String companyName) {
        log.info("Создание счета для компании: {} (ИНН: {})", companyName, inn);

        // Генерируем номер счета на основе ИНН
        String accountNumber = generateAccountNumber(inn);

        CompanyAccount account = new CompanyAccount();
        account.setInn(inn);
        account.setCompanyName(companyName);
        account.setAccountNumber(accountNumber);
        account.setBalance(new BigDecimal("500000.00")); // Стартовый баланс 500к
        account.setBik("044525225");
        account.setBankName("ПАО СБЕРБАНК");

        return accountRepository.save(account);
    }

    /**
     * Перевод денег (списание у плательщика + зачисление РЖД)
     */
    @Transactional
    public TransferResult transferMoney(String payerInn, BigDecimal amount) {
        log.info("💰 НАЧАЛО ПЕРЕВОДА: Плательщик ИНН={}, Сумма={}", payerInn, amount);

        // Получаем счета с блокировкой
        CompanyAccount payerAccount = accountRepository.findByInnForUpdate(payerInn)
                .orElseThrow(() -> new RuntimeException("Счет плательщика не найден"));

        CompanyAccount rzdAccount = accountRepository.findByInnForUpdate(RZD_INN)
                .orElseThrow(() -> new RuntimeException("Счет РЖД не найден"));

        // Сохраняем балансы ДО
        BigDecimal beforePayer = payerAccount.getBalance();
        BigDecimal beforeRzd = rzdAccount.getBalance();

        log.info("📊 БАЛАНС ДО ОПЕРАЦИИ:");
        log.info("   Плательщик ({}): {} руб", payerAccount.getCompanyName(), beforePayer);
        log.info("   РЖД: {} руб", beforeRzd);
        log.info("   Сумма перевода: {} руб", amount);

        // Проверка достаточности средств
        if (payerAccount.getBalance().compareTo(amount) < 0) {
            log.error("❌ НЕДОСТАТОЧНО СРЕДСТВ!");
            return TransferResult.failed(payerAccount, rzdAccount, amount,
                    "Недостаточно средств. Доступно: " + payerAccount.getBalance());
        }

        // Выполняем перевод
        payerAccount.setBalance(payerAccount.getBalance().subtract(amount));
        rzdAccount.setBalance(rzdAccount.getBalance().add(amount));

        accountRepository.save(payerAccount);
        accountRepository.save(rzdAccount);

        log.info("✅ ПЕРЕВОД ВЫПОЛНЕН УСПЕШНО!");
        log.info("📊 БАЛАНС ПОСЛЕ ОПЕРАЦИИ:");
        log.info("   Плательщик ({}): {} руб (было: {}, списано: {})",
                payerAccount.getCompanyName(), payerAccount.getBalance(), beforePayer, amount);
        log.info("   РЖД: {} руб (было: {}, зачислено: {})",
                rzdAccount.getBalance(), beforeRzd, amount);

        return TransferResult.success(payerAccount, rzdAccount, amount, beforePayer, beforeRzd);
    }

    /**
     * Получить баланс компании по ИНН
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String inn) {
        return accountRepository.findByInn(inn)
                .map(CompanyAccount::getBalance)
                .orElseThrow(() -> new RuntimeException("Счет не найден"));
    }

    /**
     * Генерация номера счета (упрощенно)
     */
    private String generateAccountNumber(String inn) {
        return "40702810" + inn.substring(0, 8) + String.format("%04d",
                (int)(Math.random() * 10000));
    }

    /**
     * Результат перевода
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TransferResult {
        private boolean success;
        private String message;
        private String payerInn;
        private String payerName;
        private BigDecimal payerBalanceBefore;
        private BigDecimal payerBalanceAfter;
        private BigDecimal rzdBalanceBefore;
        private BigDecimal rzdBalanceAfter;
        private BigDecimal amount;

        public static TransferResult success(CompanyAccount payer, CompanyAccount rzd,
                                             BigDecimal amount, BigDecimal beforePayer, BigDecimal beforeRzd) {
            return new TransferResult(true, "Перевод выполнен успешно",
                    payer.getInn(), payer.getCompanyName(),
                    beforePayer, payer.getBalance(),
                    beforeRzd, rzd.getBalance(), amount);
        }

        public static TransferResult failed(CompanyAccount payer, CompanyAccount rzd,
                                            BigDecimal amount, String error) {
            return new TransferResult(false, error,
                    payer.getInn(), payer.getCompanyName(),
                    payer.getBalance(), payer.getBalance(),
                    rzd.getBalance(), rzd.getBalance(), amount);
        }

        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append("══════════════════════════════════════════════════════════════\n");
            sb.append(success ? "✅ ПЕРЕВОД ВЫПОЛНЕН УСПЕШНО\n" : "❌ ОШИБКА ПЕРЕВОДА\n");
            sb.append("══════════════════════════════════════════════════════════════\n");
            sb.append(String.format("Плательщик: %s (ИНН: %s)\n", payerName, payerInn));
            sb.append(String.format("Сумма: %,8.2f руб\n", amount));

            if (success) {
                sb.append("\n📊 ДВИЖЕНИЕ ДЕНЕГ:\n");
                sb.append(String.format("   Плательщик: %,8.2f руб → %,8.2f руб (списано: %,8.2f)\n",
                        payerBalanceBefore, payerBalanceAfter, amount));
                sb.append(String.format("   РЖД:        %,8.2f руб → %,8.2f руб (зачислено: %,8.2f)\n",
                        rzdBalanceBefore, rzdBalanceAfter, amount));
            } else {
                sb.append("\n❌ Причина: ").append(message);
            }
            sb.append("══════════════════════════════════════════════════════════════\n");
            return sb.toString();
        }
    }
}