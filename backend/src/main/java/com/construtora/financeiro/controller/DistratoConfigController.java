package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.distrato.DistratoConfigRequest;
import com.construtora.financeiro.dto.distrato.DistratoConfigResponse;
import com.construtora.financeiro.service.distrato.DistratoConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/distrato-config")
@Tag(name = "Distrato Config", description = "Regra financeira de distrato (global e por empreendimento)")
public class DistratoConfigController {

    private final DistratoConfigService service;

    public DistratoConfigController(DistratoConfigService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as configurações de distrato (global + por empreendimento)")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public List<DistratoConfigResponse> list() {
        return service.list();
    }

    @PutMapping
    @Operation(summary = "Cria/atualiza a regra (global se developmentId nulo)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public DistratoConfigResponse upsert(@Valid @RequestBody DistratoConfigRequest request) {
        return service.upsert(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a regra específica de um empreendimento (a global não pode ser removida)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
