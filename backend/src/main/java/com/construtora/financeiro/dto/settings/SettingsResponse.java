package com.construtora.financeiro.dto.settings;

public record SettingsResponse(
        String systemName,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String theme,
        String companyName,
        String companyDocument,
        String companyAddress,
        String companyPhone,
        String companyEmail,
        String footerText
) {}
