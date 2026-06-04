package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.bank.BankAccountRequest;
import com.construtora.financeiro.dto.bank.BankAccountResponse;
import com.construtora.financeiro.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank-accounts")
@Tag(name = "Bank Accounts", description = "Contas bancárias")
public class BankAccountController {

    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista contas bancárias")
    @PreAuthorize("hasAuthority('CONCILIACAO_VIEW')")
    public List<BankAccountResponse> list() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Cria conta bancária")
    @PreAuthorize("hasAuthority('CONCILIACAO_EDIT')")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse create(@Valid @RequestBody BankAccountRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza conta bancária")
    @PreAuthorize("hasAuthority('CONCILIACAO_EDIT')")
    public BankAccountResponse update(@PathVariable UUID id, @Valid @RequestBody BankAccountRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove conta bancária")
    @PreAuthorize("hasAuthority('CONCILIACAO_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
