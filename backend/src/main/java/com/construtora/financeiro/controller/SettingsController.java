package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.settings.PublicSettingsResponse;
import com.construtora.financeiro.dto.settings.SettingsRequest;
import com.construtora.financeiro.dto.settings.SettingsResponse;
import com.construtora.financeiro.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Personalização visual e dados da empresa")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping("/public")
    @Operation(summary = "Branding público (tela de login) — sem dados sensíveis")
    public PublicSettingsResponse getPublic() {
        return service.getPublic();
    }

    @GetMapping
    @Operation(summary = "Configuração completa (inclui SMTP, sem a senha)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public SettingsResponse get() {
        return service.get();
    }

    @PutMapping
    @Operation(summary = "Atualiza a configuração do sistema (ADMIN)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return service.update(request);
    }
}
