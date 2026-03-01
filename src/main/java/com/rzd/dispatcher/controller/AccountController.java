package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.CreateAccountRequest;
import com.rzd.dispatcher.model.dto.request.TransferRequest;
import com.rzd.dispatcher.model.dto.response.AccountResponse;
import com.rzd.dispatcher.model.dto.response.TransferResponse;
import com.rzd.dispatcher.model.entity.CompanyAccount;
import com.rzd.dispatcher.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Создать новый счет для компании
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        CompanyAccount account = accountService.createAccount(
                request.getInn(),
                request.getCompanyName(),
                request.getBik(),
                request.getBankName(),
                request.getIsMain()
        );
        return ResponseEntity.ok(mapToResponse(account));
    }

    /**
     * Получить все счета компании по ИНН
     */
    @GetMapping("/company/{inn}")
    public ResponseEntity<List<AccountResponse>> getCompanyAccounts(@PathVariable String inn) {
        List<CompanyAccount> accounts = accountService.getAccountsByInn(inn);
        return ResponseEntity.ok(
                accounts.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Получить счет по номеру
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        CompanyAccount account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(mapToResponse(account));
    }


    /**
     * Получить баланс счета
     */
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber) {
        BigDecimal balance = accountService.getBalance(accountNumber);
        return ResponseEntity.ok(balance);
    }

    /**
     * Выполнить перевод между счетами
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferMoney(@RequestBody TransferRequest request) {
        AccountService.TransferResult result = accountService.transferMoney(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                request.getDescription()
        );

        return ResponseEntity.ok(mapToTransferResponse(result));
    }



    private AccountResponse mapToResponse(CompanyAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .inn(account.getInn())
                .companyName(account.getCompanyName())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .bik(account.getBik())
                .bankName(account.getBankName())
                .isMain(account.getIsMain())
                .isRzdAccount(account.getIsRzdAccount())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private TransferResponse mapToTransferResponse(AccountService.TransferResult result) {
        return TransferResponse.builder()
                .success(result.isSuccess())
                .message(result.getMessage())
                .fromAccountNumber(result.getFromAccountNumber())
                .fromInn(result.getFromInn())
                .fromName(result.getFromName())
                .toAccountNumber(result.getToAccountNumber())
                .toInn(result.getToInn())
                .toName(result.getToName())
                .fromBalanceBefore(result.getFromBalanceBefore())
                .fromBalanceAfter(result.getFromBalanceAfter())
                .toBalanceBefore(result.getToBalanceBefore())
                .toBalanceAfter(result.getToBalanceAfter())
                .amount(result.getAmount())
                .description(result.getDescription())
                .build();
    }
}