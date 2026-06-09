package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.distrato.*;
import com.construtora.financeiro.model.enums.DistratoStatus;
import com.construtora.financeiro.service.distrato.DistratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/distratos")
@Tag(name = "Distratos", description = "Fluxo completo de distrato de lote/terreno")
public class DistratoController {

    private final DistratoService service;

    public DistratoController(DistratoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista distratos (filtro opcional ?status=)")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public List<DistratoResponse> list(@RequestParam(required = false) DistratoStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um distrato")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public DistratoResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simula o distrato (sem persistir) — exibe a memória de cálculo")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public DistratoSimulationResponse simulate(@Valid @RequestBody DistratoSimulationRequest request) {
        return service.simulate(request);
    }

    @PostMapping
    @Operation(summary = "Solicita o distrato (status SOLICITADO; bloqueia o lote em EM_DISTRATO)")
    @PreAuthorize("hasAuthority('VENDAS_EDIT')")
    @ResponseStatus(HttpStatus.CREATED)
    public DistratoResponse request(@Valid @RequestBody DistratoCreateRequest request) {
        return service.request(request);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprova o distrato; gera Contas a Pagar/Receber ou conclui se saldo zero")
    @PreAuthorize("hasAuthority('VENDAS_EDIT')")
    public DistratoResponse approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "Registra a quitação financeira (baixa AP/AR) e conclui o distrato")
    @PreAuthorize("hasAuthority('VENDAS_EDIT')")
    public DistratoResponse settle(@PathVariable UUID id,
                                   @RequestBody(required = false) DistratoSettleRequest request) {
        return service.settle(id, request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela o distrato; libera o lote de volta a VENDIDO")
    @PreAuthorize("hasAuthority('VENDAS_EDIT')")
    public DistratoResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
