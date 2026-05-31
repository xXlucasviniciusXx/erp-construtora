package com.construtora.financeiro.service;

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
                s.getCompanyEmail(), s.getFooterText());
    }
}
