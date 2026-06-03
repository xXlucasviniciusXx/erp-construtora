package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.settings.PublicSettingsResponse;
import com.construtora.financeiro.dto.settings.SettingsRequest;
import com.construtora.financeiro.dto.settings.SettingsResponse;
import com.construtora.financeiro.model.SystemSettings;
import com.construtora.financeiro.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {

    private final SystemSettingsRepository repository;

    public SettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return toResponse(current());
    }

    @Transactional(readOnly = true)
    public PublicSettingsResponse getPublic() {
        SystemSettings s = current();
        return new PublicSettingsResponse(s.getSystemName(), s.getLogoUrl(), s.getPrimaryColor(),
                s.getSecondaryColor(), s.getTheme(), s.getFooterText());
    }

    public SettingsResponse update(SettingsRequest r) {
        SystemSettings s = current();
        s.setSystemName(r.systemName());
        s.setLogoUrl(r.logoUrl());
        if (r.primaryColor() != null) s.setPrimaryColor(r.primaryColor());
        if (r.secondaryColor() != null) s.setSecondaryColor(r.secondaryColor());
        if (r.theme() != null) s.setTheme(r.theme());
        s.setCompanyName(r.companyName());
        s.setCompanyDocument(r.companyDocument());
        s.setCompanyAddress(r.companyAddress());
        s.setCompanyPhone(r.companyPhone());
        s.setCompanyEmail(r.companyEmail());
        s.setFooterText(r.footerText());
        // ---- SMTP ----
        if (r.mailEnabled() != null) s.setMailEnabled(r.mailEnabled());
        s.setMailHost(r.mailHost());
        if (r.mailPort() != null) s.setMailPort(r.mailPort());
        s.setMailUsername(r.mailUsername());
        // senha só é atualizada quando enviada (campo write-only)
        if (r.mailPassword() != null && !r.mailPassword().isBlank()) s.setMailPassword(r.mailPassword());
        s.setMailFrom(r.mailFrom());
        if (r.mailReminderDays() != null) s.setMailReminderDays(r.mailReminderDays());
        return toResponse(repository.save(s));
    }

    /** Sempre existe uma única linha (criada na migration); cria fallback se ausente. */
    public SystemSettings current() {
        return repository.findAll().stream().findFirst().orElseGet(() -> repository.save(new SystemSettings()));
    }

    private SettingsResponse toResponse(SystemSettings s) {
        return new SettingsResponse(
                s.getSystemName(), s.getLogoUrl(), s.getPrimaryColor(), s.getSecondaryColor(), s.getTheme(),
                s.getCompanyName(), s.getCompanyDocument(), s.getCompanyAddress(), s.getCompanyPhone(),
                s.getCompanyEmail(), s.getFooterText(),
                s.isMailEnabled(), s.getMailHost(), s.getMailPort(), s.getMailUsername(),
                s.getMailFrom(), s.getMailReminderDays(),
                s.getMailPassword() != null && !s.getMailPassword().isBlank());
    }
}
