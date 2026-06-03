package com.construtora.financeiro.dto.settings;

import jakarta.validation.constraints.NotBlank;

public record SettingsRequest(
        @NotBlank String systemName,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String theme,
        String companyName,
        String companyDocument,
        String companyAddress,
        String companyPhone,
        String companyEmail,
        String footerText,
        // ---- SMTP ----
        Boolean mailEnabled,
        String mailHost,
        Integer mailPort,
        String mailUsername,
        String mailPassword,   // só atualiza quando enviado (não-vazio)
        String mailFrom,
        Integer mailReminderDays
) {}
