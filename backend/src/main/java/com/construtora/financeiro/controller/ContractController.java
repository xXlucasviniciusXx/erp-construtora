package com.construtora.financeiro.controller;

import com.construtora.financeiro.service.contract.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contracts", description = "Geração de contratos (HTML/PDF)")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping(value = "/sales/{saleId}/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Gera contrato em HTML")
    @PreAuthorize("hasAnyAuthority('CONTRACTS_GENERATE','READ')")
    public ResponseEntity<String> html(@PathVariable UUID saleId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(service.generateHtml(saleId));
    }

    @GetMapping(value = "/sales/{saleId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Gera contrato em PDF")
    @PreAuthorize("hasAuthority('CONTRACTS_GENERATE')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID saleId) {
        byte[] pdf = service.generatePdf(saleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contrato-" + saleId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
