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
        String footerText,
        // ---- SMTP (senha nunca é retornada; só indica se está definida) ----
        Boolean mailEnabled,
        String mailHost,
        Integer mailPort,
        String mailUsername,
        String mailFrom,
        Integer mailReminderDays,
        Boolean mailPasswordSet
) {}
