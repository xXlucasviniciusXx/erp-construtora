package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.dashboard.Point;
import com.construtora.financeiro.dto.dre.DreResponse;
import com.construtora.financeiro.service.DreService;
import com.construtora.financeiro.service.report.CsvWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dre")
@Tag(name = "DRE", description = "Demonstração do Resultado (base caixa)")
public class DreController {

    private final DreService service;

    public DreController(DreService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "DRE por período, empreendimento e base (CAIXA ou COMPETENCIA)")
    @PreAuthorize("hasAuthority('DRE_VIEW')")
    public DreResponse dre(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(required = false) UUID developmentId,
                           @RequestParam(required = false, defaultValue = "CAIXA") DreService.Basis basis) {
        return service.dre(from, to, developmentId, basis);
    }

    @GetMapping("/export")
    @Operation(summary = "Exporta o DRE em CSV")
    @PreAuthorize("hasAuthority('DRE_VIEW')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) UUID developmentId,
                                         @RequestParam(required = false, defaultValue = "CAIXA") DreService.Basis basis) {
        DreResponse d = service.dre(from, to, developmentId, basis);
        List<List<Object>> rows = new ArrayList<>();
        for (Point r : d.revenues()) rows.add(List.of("RECEITA", r.label(), r.value()));
        rows.add(List.of("RECEITA", "TOTAL DE RECEITAS", d.totalRevenue()));
        for (Point e : d.expenses()) rows.add(List.of("DESPESA", e.label(), e.value()));
        rows.add(List.of("DESPESA", "TOTAL DE DESPESAS", d.totalExpense()));
        rows.add(List.of("RESULTADO", "LUCRO/PREJUÍZO", d.result()));

        String content = CsvWriter.build(List.of("Tipo", "Linha", "Valor"), rows);
        byte[] bytes = ("﻿" + content).getBytes(StandardCharsets.UTF_8); // BOM p/ Excel
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dre.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }
}
