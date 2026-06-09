package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.bank.BankTransactionResponse;
import com.construtora.financeiro.model.enums.TransactionStatus;
import com.construtora.financeiro.service.BankTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Lista transações por conta ou por status (paginado)")
    @PreAuthorize("hasAuthority('CONCILIACAO_VIEW')")
    public Page<BankTransactionResponse> list(@RequestParam(required = false) UUID bankAccountId,
                                              @RequestParam(required = false) TransactionStatus status,
                                              @PageableDefault(size = 20) Pageable pageable) {
        if (bankAccountId != null && status != null) {
            return service.findByAccountAndStatus(bankAccountId, status, pageable);
        }
        if (bankAccountId != null) {
            return service.findByAccount(bankAccountId, pageable);
        }
        if (status != null) {
            return service.findByStatus(status, pageable);
        }
        return service.findByStatus(TransactionStatus.PENDING, pageable);
    }
}
