package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.costcenter.CostCenterRequest;
import com.construtora.financeiro.dto.costcenter.CostCenterResponse;
import com.construtora.financeiro.service.CostCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cost-centers")
@Tag(name = "Cost Centers", description = "Cadastro de centros de custo")
public class CostCenterController {

    private final CostCenterService service;

    public CostCenterController(CostCenterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista centros de custo")
    @PreAuthorize("hasAuthority('CONTAS_PAGAR_VIEW')")
    public List<CostCenterResponse> list() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Cria centro de custo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CostCenterResponse create(@Valid @RequestBody CostCenterRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza centro de custo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public CostCenterResponse update(@PathVariable UUID id, @Valid @RequestBody CostCenterRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove centro de custo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
