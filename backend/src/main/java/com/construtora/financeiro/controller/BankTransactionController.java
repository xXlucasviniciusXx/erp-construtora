package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.bank.BankTransactionResponse;
import com.construtora.financeiro.model.enums.TransactionStatus;
import com.construtora.financeiro.service.BankTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank-transactions")
@Tag(name = "Bank Transactions", description = "Transações bancárias importadas")
public class BankTransactionController {

    private final BankTransactionService service;

    public BankTransactionController(BankTransactionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista transações por conta ou por status")
    @PreAuthorize("hasAuthority('READ')")
    public List<BankTransactionResponse> list(@RequestParam(required = false) UUID bankAccountId,
                                              @RequestParam(required = false) TransactionStatus status) {
        if (status != null) {
            return service.findByStatus(status);
        }
        if (bankAccountId != null) {
            return service.findByAccount(bankAccountId);
        }
        return service.findByStatus(TransactionStatus.PENDING);
    }
}
