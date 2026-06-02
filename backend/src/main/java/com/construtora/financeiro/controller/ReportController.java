package com.construtora.financeiro.controller;

import com.construtora.financeiro.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Relatórios exportáveis em CSV")
@PreAuthorize("hasAnyAuthority('REPORTS_EXPORT','READ')")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/accounts-payable")
    @Operation(summary = "Contas a pagar por período (CSV)")
    public ResponseEntity<byte[]> payable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return csv("contas-a-pagar.csv", service.payableByPeriod(start, end));
    }

    @GetMapping("/accounts-receivable")
    @Operation(summary = "Contas a receber por período (CSV)")
    public ResponseEntity<byte[]> receivable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return csv("contas-a-receber.csv", service.receivableByPeriod(start, end));
    }

    @GetMapping("/overdue-installments")
    @Operation(summary = "Parcelas em atraso (CSV)")
    public ResponseEntity<byte[]> overdue() {
        return csv("parcelas-em-atraso.csv", service.overdueInstallments());
    }

    @GetMapping("/reconciliations")
    @Operation(summary = "Conciliações realizadas (CSV)")
    public ResponseEntity<byte[]> reconciliations() {
        return csv("conciliacoes.csv", service.reconciliations());
    }

    @GetMapping("/pending-transactions")
    @Operation(summary = "Transações pendentes (CSV)")
    public ResponseEntity<byte[]> pending() {
        return csv("transacoes-pendentes.csv", service.pendingTransactions());
    }

    @GetMapping("/sales-by-development")
    @Operation(summary = "Vendas por empreendimento (CSV)")
    public ResponseEntity<byte[]> salesByDevelopment() {
        return csv("vendas-por-empreendimento.csv", service.salesByDevelopment());
    }

    @GetMapping("/expenses-by-development")
    @Operation(summary = "Despesas pagas por empreendimento (CSV)")
    public ResponseEntity<byte[]> expensesByDevelopment() {
        return csv("despesas-por-empreendimento.csv", service.expensesByDevelopment());
    }

    @GetMapping("/delinquent-clients")
    @Operation(summary = "Clientes inadimplentes (CSV)")
    public ResponseEntity<byte[]> delinquent() {
        return csv("clientes-inadimplentes.csv", service.delinquentClients());
    }

    private ResponseEntity<byte[]> csv(String filename, String content) {
        byte[] bytes = ("﻿" + content).getBytes(StandardCharsets.UTF_8); // BOM p/ Excel
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }
}
