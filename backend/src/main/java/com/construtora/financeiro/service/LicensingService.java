package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.licensing.*;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.License;
import com.construtora.financeiro.model.Module;
import com.construtora.financeiro.repository.LicenseRepository;
import com.construtora.financeiro.repository.ModuleRepository;
import com.construtora.financeiro.service.licensing.LicenseClaims;
import com.construtora.financeiro.service.licensing.LicenseKeyService;
import com.construtora.financeiro.service.licensing.PlanModules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestão de módulos (feature flags), licença e chave de licenciamento.
 * Calcula também o estado de enforcement (módulos ativos + bloqueio por
 * vencimento) consumido pelo {@code LicenseEnforcementInterceptor}.
 */
@Service
@Transactional
public class LicensingService {

    private final ModuleRepository moduleRepository;
    private final LicenseRepository licenseRepository;
    private final LicenseKeyService keyService;

    public LicensingService(ModuleRepository moduleRepository, LicenseRepository licenseRepository,
                            LicenseKeyService keyService) {
        this.moduleRepository = moduleRepository;
        this.licenseRepository = licenseRepository;
        this.keyService = keyService;
    }

    @Transactional(readOnly = true)
    public LicensingInfo info() {
        return new LicensingInfo(modules(), license());
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> modules() {
        return moduleRepository.findAllByOrderBySortOrder().stream().map(this::toResponse).toList();
    }

    public ModuleResponse toggleModule(String code, boolean active) {
        Module m = moduleRepository.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Módulo", code));
        m.setActive(active);
        return toResponse(moduleRepository.save(m));
    }

    @Transactional(readOnly = true)
    public LicenseResponse license() {
        return toResponse(current());
    }

    public LicenseResponse updateLicense(LicenseRequest r) {
        License l = current();
        l.setPlan(r.plan());
        l.setStatus(r.status());
        if (r.startDate() != null) l.setStartDate(r.startDate());
        l.setEndDate(r.endDate());
        if (r.periodMonths() != null) l.setPeriodMonths(r.periodMonths());
        l.setMaxUsers(r.maxUsers());
        l.setNotes(r.notes());
        return toResponse(licenseRepository.save(l));
    }

    /** Liga o pacote de módulos de um plano (e desliga os demais geridos por plano). */
    public LicensingInfo applyPlan(String plan) {
        if (!PlanModules.isValidPlan(plan)) {
            throw new BusinessException("Plano inválido: " + plan);
        }
        applyModulesForPlan(plan.toUpperCase());
        License l = current();
        l.setPlan(plan.toUpperCase());
        licenseRepository.save(l);
        return info();
    }

    /** Aplica uma chave assinada: valida, grava a licença e liga o pacote do plano. */
    public LicensingInfo applyKey(String key) {
        LicenseClaims claims = keyService.parse(key);
        License l = current();
        l.setPlan(claims.plan().toUpperCase());
        l.setStatus("ATIVA");
        l.setCustomer(claims.customer());
        if (claims.startDate() != null) l.setStartDate(claims.startDate());
        Integer months = claims.periodMonths();
        if (months != null) l.setPeriodMonths(months);
        LocalDate start = l.getStartDate() != null ? l.getStartDate() : LocalDate.now();
        if (claims.endDate() != null) {
            l.setEndDate(claims.endDate());
        } else if (months != null) {
            l.setEndDate(start.plusMonths(months));
        }
        l.setMaxUsers(claims.maxUsers());
        l.setLicenseKey(key.trim());
        licenseRepository.save(l);
        applyModulesForPlan(claims.plan().toUpperCase());
        return info();
    }

    @Transactional(readOnly = true)
    public String generateKey(LicenseKeyGenerateRequest r) {
        if (!PlanModules.isValidPlan(r.plan())) {
            throw new BusinessException("Plano inválido: " + r.plan());
        }
        LocalDate start = r.startDate() != null ? r.startDate() : LocalDate.now();
        Integer months = r.periodMonths() != null ? r.periodMonths() : 12;
        LocalDate end = start.plusMonths(months);
        return keyService.generate(new LicenseClaims(
                r.plan().toUpperCase(), r.customer(), start, end, months, r.maxUsers()));
    }

    // ---- Estado de enforcement (consumido pelo interceptor) ----

    @Transactional(readOnly = true)
    public Set<String> activeModuleCodes() {
        return moduleRepository.findAllByOrderBySortOrder().stream()
                .filter(Module::isActive).map(Module::getCode).collect(Collectors.toSet());
    }

    /** Estado de bloqueio por vencimento/status da licença. */
    @Transactional(readOnly = true)
    public LicenseGate gate() {
        License l = current();
        String status = l.getStatus() == null ? "ATIVA" : l.getStatus().toUpperCase();
        if ("CANCELADA".equals(status)) return new LicenseGate(true, true);
        if ("SUSPENSA".equals(status)) return new LicenseGate(false, true);
        if (l.getEndDate() != null) {
            LocalDate readOnlyFrom = l.getEndDate().plusDays(l.getGraceDays());
            if (LocalDate.now().isAfter(readOnlyFrom)) return new LicenseGate(false, true);
        }
        return new LicenseGate(false, false);
    }

    /** blocked = nega tudo; readOnly = permite só leitura (GET). */
    public record LicenseGate(boolean blocked, boolean readOnly) {}

    // ---- helpers ----

    private void applyModulesForPlan(String plan) {
        Set<String> allowed = PlanModules.modulesFor(plan);
        for (Module m : moduleRepository.findAllByOrderBySortOrder()) {
            if (PlanModules.ALL.contains(m.getCode())) {
                m.setActive(allowed.contains(m.getCode()));
                moduleRepository.save(m);
            }
        }
    }

    /** Sempre existe uma linha (criada na migration); cria fallback se ausente. */
    private License current() {
        return licenseRepository.findAll().stream().findFirst().orElseGet(() -> licenseRepository.save(new License()));
    }

    private ModuleResponse toResponse(Module m) {
        return new ModuleResponse(m.getId(), m.getCode(), m.getName(), m.getDescription(),
                m.isActive(), m.getSortOrder());
    }

    private LicenseResponse toResponse(License l) {
        LocalDate today = LocalDate.now();
        String rawStatus = l.getStatus() == null ? "ATIVA" : l.getStatus().toUpperCase();
        boolean expired = l.getEndDate() != null && l.getEndDate().isBefore(today);
        Long daysToExpire = l.getEndDate() == null ? null : ChronoUnit.DAYS.between(today, l.getEndDate());
        LicenseGate g = gate();
        String effective;
        if ("CANCELADA".equals(rawStatus)) effective = "CANCELADA";
        else if ("SUSPENSA".equals(rawStatus)) effective = "SUSPENSA";
        else if (expired) effective = "EXPIRADA";
        else effective = "ATIVA";
        return new LicenseResponse(l.getPlan(), effective, l.getStartDate(), l.getEndDate(),
                l.getPeriodMonths(), l.getMaxUsers(), l.getNotes(), l.getCustomer(), l.getGraceDays(),
                daysToExpire, expired, g.readOnly(), g.blocked(),
                l.getLicenseKey() != null && !l.getLicenseKey().isBlank());
    }
}
