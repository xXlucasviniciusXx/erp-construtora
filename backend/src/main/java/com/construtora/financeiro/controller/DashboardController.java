package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.dashboard.DashboardAnalyticsResponse;
import com.construtora.financeiro.dto.dashboard.DashboardResponse;
import com.construtora.financeiro.service.DashboardAnalyticsService;
import com.construtora.financeiro.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores da tela inicial")
public class DashboardController {

    private final DashboardService service;
    private final DashboardAnalyticsService analyticsService;

    public DashboardController(DashboardService service, DashboardAnalyticsService analyticsService) {
        this.service = service;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Indicadores consolidados")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public DashboardResponse summary() {
        return service.summary();
    }

    @GetMapping("/analytics")
    @Operation(summary = "Cards e séries para os gráficos do dashboard (filtros opcionais)")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public DashboardAnalyticsResponse analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID propertyId) {
        return analyticsService.analytics(from, to, clientId, propertyId);
    }
}
