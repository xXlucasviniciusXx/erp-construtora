package com.construtora.financeiro.controller;

import com.construtora.financeiro.model.SystemSettings;
import com.construtora.financeiro.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Serve arquivos estáticos armazenados no banco (logo, etc.).
 * Não exige autenticação — o logo é exibido na tela de login.
 */
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Arquivos estáticos (logo) armazenados no banco")
public class AssetController {

    private final SettingsService settingsService;

    public AssetController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/logo")
    @Operation(summary = "Retorna o logo do sistema armazenado no banco (sem auth)")
    public ResponseEntity<byte[]> logo() {
        SystemSettings s = settingsService.current();
        if (s.getLogoData() == null || s.getLogoData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.parseMediaType(
                s.getLogoMime() != null ? s.getLogoMime() : "image/png");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(s.getLogoData());
    }
}
