package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.dashboard.DashboardResponse;
import com.construtora.financeiro.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores da tela inicial")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Indicadores consolidados")
    @PreAuthorize("hasAuthority('READ')")
    public DashboardResponse summary() {
        return service.summary();
    }
}
