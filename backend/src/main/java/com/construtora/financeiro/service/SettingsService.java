package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.settings.PublicSettingsResponse;
import com.construtora.financeiro.dto.settings.SettingsRequest;
import com.construtora.financeiro.dto.settings.SettingsResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.model.SystemSettings;
import com.construtora.financeiro.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

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
        return new PublicSettingsResponse(s.getSystemName(), versionedLogoUrl(s), s.getPrimaryColor(),
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

    private static final Set<String> ALLOWED_MIME = Set.of("image/png", "image/jpeg", "image/gif", "image/svg+xml", "image/webp");
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024; // 2 MB

    /** Faz upload do logo e o armazena no banco. Retorna a config atualizada. */
    public SettingsResponse uploadLogo(MultipartFile file) throws IOException {
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!ALLOWED_MIME.contains(mime)) {
            throw new BusinessException("Tipo de arquivo não permitido. Use PNG, JPEG, GIF, SVG ou WebP.");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("O arquivo é muito grande. Limite: 2 MB.");
        }
        SystemSettings s = current();
        s.setLogoData(file.getBytes());
        s.setLogoMime(mime);
        // Aponta logo_url para o endpoint que serve o logo armazenado
        s.setLogoUrl("/api/assets/logo");
        return toResponse(repository.save(s));
    }

    /** Remove o logo armazenado no banco (a logo_url volta a ser o campo manual). */
    public SettingsResponse deleteLogo() {
        SystemSettings s = current();
        s.setLogoData(null);
        s.setLogoMime(null);
        if ("/api/assets/logo".equals(s.getLogoUrl())) s.setLogoUrl(null);
        return toResponse(repository.save(s));
    }

    /** Sempre existe uma única linha (criada na migration); cria fallback se ausente. */
    public SystemSettings current() {
        return repository.findAll().stream().findFirst().orElseGet(() -> repository.save(new SystemSettings()));
    }

    /**
     * Acrescenta um parâmetro de versão (?v=updatedAt) ao logo interno, para que
     * login/layout busquem a imagem nova quando o logo muda, mantendo o cache de
     * 1h por versão. URLs externas (http/https) e nulas passam sem alteração.
     */
    private String versionedLogoUrl(SystemSettings s) {
        String url = s.getLogoUrl();
        if (url == null || url.isBlank() || !url.startsWith("/api/assets/")) return url;
        long v = s.getUpdatedAt() != null ? s.getUpdatedAt().toEpochSecond() : 0L;
        return url + "?v=" + v;
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
