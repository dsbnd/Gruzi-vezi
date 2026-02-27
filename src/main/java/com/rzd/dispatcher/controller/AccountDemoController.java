package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.response.PaymentResponse;
import com.rzd.dispatcher.service.AccountService;
import com.rzd.dispatcher.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/demo/accounts")
@RequiredArgsConstructor
public class AccountDemoController {

    private final AccountService accountService;
    private final PaymentService paymentService;


    @GetMapping("/pay")
    public String processPaymentWithMoneyTransfer(@RequestParam UUID paymentId) {

        // 1. Получаем платеж
        PaymentResponse payment = paymentService.getPaymentStatus(paymentId);

        // 2. Переводим деньги (списание со счета компании → зачисление РЖД)
        AccountService.TransferResult transfer = accountService.transferMoney(
                payment.getInn(), payment.getAmount());

        // 3. Если перевод успешен - отправляем вебхук в систему
        if (transfer.isSuccess()) {
            paymentService.sendSuccessWebhook(paymentId);
        }

        // 4. Возвращаем отчет
        return transfer.formatReport();
    }

    /**
     * Проверить баланс компании по ИНН
     */
    @GetMapping("/balance/{inn}")
    public String getBalance(@PathVariable String inn) {
        BigDecimal balance = accountService.getBalance(inn);
        return String.format("Баланс компании с ИНН %s: %,8.2f руб", inn, balance);
    }

    /**
     * Создать счет для новой компании (при регистрации)
     */
    @PostMapping("/create")
    public String createAccount(@RequestParam String inn, @RequestParam String companyName) {
        accountService.createAccountForCompany(inn, companyName);
        return String.format("Счет создан для компании %s (ИНН: %s)", companyName, inn);
    }
}