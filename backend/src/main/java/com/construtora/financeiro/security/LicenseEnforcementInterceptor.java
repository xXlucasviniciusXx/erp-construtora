package com.construtora.financeiro.security;

import com.construtora.financeiro.exception.ApiError;
import com.construtora.financeiro.service.LicensingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Enforcement do licenciamento (Fase 2), em duas camadas:
 *   1) Licença: se CANCELADA → bloqueia tudo; se vencida além da tolerância ou
 *      SUSPENSA → modo só-leitura (só GET passa).
 *   2) Módulos: recusa o acesso a um endpoint cujo módulo está desligado no plano.
 *
 * Caminhos de núcleo (auth, licensing, settings, users, roles) ficam SEMPRE
 * liberados, garantindo que o admin consiga logar e renovar/ajustar a licença.
 */
@Component
public class LicenseEnforcementInterceptor implements HandlerInterceptor {

    /** Prefixos isentos de qualquer enforcement (recuperação/administração). */
    private static final List<String> EXEMPT = List.of(
            "/api/auth", "/api/licensing", "/api/settings", "/api/users", "/api/roles");

    /** Prefixo de caminho → código do módulo. Ordem não importa (sem sobreposição ambígua). */
    private static final Map<String, String> PATH_MODULE = Map.ofEntries(
            Map.entry("/api/clients", "CLIENTES"),
            Map.entry("/api/developments", "EMPREENDIMENTOS"),
            Map.entry("/api/blocks", "EMPREENDIMENTOS"),
            Map.entry("/api/lots", "EMPREENDIMENTOS"),
            Map.entry("/api/sales", "VENDAS"),
            Map.entry("/api/installments", "VENDAS"),
            Map.entry("/api/contracts", "VENDAS"),
            Map.entry("/api/accounts-payable", "CONTAS_PAGAR"),
            Map.entry("/api/categories", "CONTAS_PAGAR"),
            Map.entry("/api/cost-centers", "CONTAS_PAGAR"),
            Map.entry("/api/accounts-receivable", "CONTAS_RECEBER"),
            Map.entry("/api/suppliers", "FORNECEDORES"),
            Map.entry("/api/reconciliation", "CONCILIACAO"),
            Map.entry("/api/bank-accounts", "CONCILIACAO"),
            Map.entry("/api/bank-transactions", "CONCILIACAO"),
            Map.entry("/api/dre", "DRE"),
            Map.entry("/api/reports", "RELATORIOS"),
            Map.entry("/api/dashboard", "DASHBOARD"),
            Map.entry("/api/notifications", "NOTIFICACOES"));

    private final LicensingService licensingService;
    private final ObjectMapper objectMapper;

    public LicenseEnforcementInterceptor(LicensingService licensingService, ObjectMapper objectMapper) {
        this.licensingService = licensingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (!uri.startsWith("/api/") || "OPTIONS".equals(method)) return true;
        for (String ex : EXEMPT) {
            if (uri.startsWith(ex)) return true;
        }

        // 1) Gate da licença
        LicensingService.LicenseGate gate = licensingService.gate();
        boolean write = !("GET".equals(method) || "HEAD".equals(method));
        if (gate.blocked()) {
            return deny(response, uri, "Licença cancelada. Acesso bloqueado — renove para continuar.");
        }
        if (gate.readOnly() && write) {
            return deny(response, uri, "Licença vencida ou suspensa. Sistema em modo somente-leitura até a renovação.");
        }

        // 2) Gate do módulo
        String module = moduleFor(uri);
        if (module != null && !licensingService.activeModuleCodes().contains(module)) {
            return deny(response, uri, "Módulo não disponível no seu plano: " + module);
        }
        return true;
    }

    private String moduleFor(String uri) {
        for (Map.Entry<String, String> e : PATH_MODULE.entrySet()) {
            if (uri.equals(e.getKey()) || uri.startsWith(e.getKey() + "/") || uri.startsWith(e.getKey() + "?")) {
                return e.getValue();
            }
        }
        return null;
    }

    private boolean deny(HttpServletResponse response, String path, String message) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(OffsetDateTime.now(), HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(), message, path, null);
        objectMapper.writeValue(response.getOutputStream(), body);
        return false;
    }
}
