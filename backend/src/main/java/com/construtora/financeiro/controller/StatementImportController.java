package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.bank.ImportResultResponse;
import com.construtora.financeiro.service.StatementImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank-accounts/{bankAccountId}/imports")
@Tag(name = "Bank Statement Import", description = "Importação de extratos (CSV/OFX)")
public class StatementImportController {

    private final StatementImportService service;

    public StatementImportController(StatementImportService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Histórico de importações da conta")
    @PreAuthorize("hasAuthority('READ')")
    public List<ImportResultResponse> history(@PathVariable UUID bankAccountId) {
        return service.history(bankAccountId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Importa um arquivo de extrato (.csv ou .ofx)")
    @PreAuthorize("hasAuthority('RECONCILIATION_WRITE')")
    public ImportResultResponse importFile(@PathVariable UUID bankAccountId,
                                           @RequestParam("file") MultipartFile file) {
        return service.importFile(bankAccountId, file);
    }
}
