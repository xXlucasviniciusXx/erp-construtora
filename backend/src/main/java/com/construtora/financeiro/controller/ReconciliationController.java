package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.bank.BankTransactionResponse;
import com.construtora.financeiro.dto.reconciliation.ManualTargetResponse;
import com.construtora.financeiro.dto.reconciliation.ReconcileRequest;
import com.construtora.financeiro.dto.reconciliation.ReconciliationResponse;
import com.construtora.financeiro.dto.reconciliation.SuggestionResponse;
import com.construtora.financeiro.model.enums.ReconciliationMode;
import com.construtora.financeiro.model.enums.TransactionStatus;
import com.construtora.financeiro.service.reconciliation.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@Tag(name = "Reconciliation", description = "Conciliação bancária (módulo principal)")
public class ReconciliationController {

    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    @GetMapping("/pendencies")
    @Operation(summary = "Transações pendentes de conciliação")
    @PreAuthorize("hasAuthority('READ')")
    public List<BankTransactionResponse> pendencies() {
        return service.pendencies();
    }

    @GetMapping("/history")
    @Operation(summary = "Histórico de conciliações")
    @PreAuthorize("hasAuthority('READ')")
    public List<ReconciliationResponse> history() {
        return service.history();
    }

    @GetMapping("/transactions/{transactionId}/suggestions")
    @Operation(summary = "Gera sugestões de conciliação para uma transação")
    @PreAuthorize("hasAnyAuthority('RECONCILIATION_WRITE','RECONCILIATION_VALIDATE')")
    public List<SuggestionResponse> suggestions(@PathVariable UUID transactionId) {
        return service.generateSuggestions(transactionId);
    }

    @GetMapping("/transactions/{transactionId}/targets")
    @Operation(summary = "Lista lançamentos em aberto para conciliação manual")
    @PreAuthorize("hasAnyAuthority('RECONCILIATION_WRITE','RECONCILIATION_VALIDATE')")
    public List<ManualTargetResponse> targets(@PathVariable UUID transactionId) {
        return service.manualTargets(transactionId);
    }

    @PostMapping("/transactions/{transactionId}/reconcile")
    @Operation(summary = "Concilia manualmente uma transação a um lançamento")
    @PreAuthorize("hasAuthority('RECONCILIATION_WRITE')")
    public ReconciliationResponse reconcile(@PathVariable UUID transactionId,
                                            @Valid @RequestBody ReconcileRequest request) {
        return service.reconcile(transactionId, request, ReconciliationMode.MANUAL);
    }

    @PatchMapping("/transactions/{transactionId}/status")
    @Operation(summary = "Marca transação como PENDING, IGNORED ou DIVERGENT (motivo opcional)")
    @PreAuthorize("hasAuthority('RECONCILIATION_WRITE')")
    public BankTransactionResponse updateStatus(@PathVariable UUID transactionId,
                                                @RequestParam TransactionStatus status,
                                                @RequestParam(required = false) String notes) {
        return service.updateTransactionStatus(transactionId, status, notes);
    }

    @PostMapping("/{reconciliationId}/undo")
    @Operation(summary = "Desfaz uma conciliação")
    @PreAuthorize("hasAuthority('RECONCILIATION_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void undo(@PathVariable UUID reconciliationId) {
        service.undo(reconciliationId);
    }
}
