package com.construtora.financeiro.dto.settings;

/** Apenas branding — exposto sem autenticação (tela de login). Sem dados sensíveis. */
public record PublicSettingsResponse(
        String systemName,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String theme,
        String footerText
) {}
