package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.licensing.LicenseKeyGenerateRequest;
import com.construtora.financeiro.dto.licensing.LicenseKeyRequest;
import com.construtora.financeiro.dto.licensing.LicenseKeyResponse;
import com.construtora.financeiro.dto.licensing.LicenseRequest;
import com.construtora.financeiro.dto.licensing.LicenseResponse;
import com.construtora.financeiro.dto.licensing.LicensingInfo;
import com.construtora.financeiro.dto.licensing.ModuleResponse;
import com.construtora.financeiro.dto.licensing.ModuleToggleRequest;
import com.construtora.financeiro.dto.licensing.PlanRequest;
import com.construtora.financeiro.service.LicensingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/licensing")
@Tag(name = "Licensing", description = "Módulos (feature flags) e licença da instalação")
public class LicensingController {

    private final LicensingService service;

    public LicensingController(LicensingService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Módulos ativos + licença para o usuário logado (monta o menu)")
    public LicensingInfo me() {
        return service.info();
    }

    @GetMapping("/modules")
    @Operation(summary = "Lista todos os módulos (gestão)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public List<ModuleResponse> modules() {
        return service.modules();
    }

    @PutMapping("/modules/{code}")
    @Operation(summary = "Liga/desliga um módulo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ModuleResponse toggle(@PathVariable String code, @Valid @RequestBody ModuleToggleRequest request) {
        return service.toggleModule(code, request.active());
    }

    @GetMapping("/license")
    @Operation(summary = "Licença atual (gestão)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public LicenseResponse license() {
        return service.license();
    }

    @PutMapping("/license")
    @Operation(summary = "Atualiza a licença (plano, validade, status)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public LicenseResponse updateLicense(@Valid @RequestBody LicenseRequest request) {
        return service.updateLicense(request);
    }

    @PostMapping("/license/key")
    @Operation(summary = "Aplica uma chave de licenciamento (valida e liga o pacote do plano)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public LicensingInfo applyKey(@Valid @RequestBody LicenseKeyRequest request) {
        return service.applyKey(request.key());
    }

    @PostMapping("/license/key/generate")
    @Operation(summary = "Gera uma chave assinada (uso interno/teste; Fase 5 = painel)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public LicenseKeyResponse generateKey(@Valid @RequestBody LicenseKeyGenerateRequest request) {
        return new LicenseKeyResponse(service.generateKey(request));
    }

    @PostMapping("/plan")
    @Operation(summary = "Aplica um plano manualmente (liga o pacote de módulos)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public LicensingInfo applyPlan(@Valid @RequestBody PlanRequest request) {
        return service.applyPlan(request.plan());
    }
}
