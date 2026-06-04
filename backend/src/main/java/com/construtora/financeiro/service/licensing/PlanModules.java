package com.construtora.financeiro.service.licensing;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapa Plano → conjunto de módulos liberados (cumulativo).
 * Aplicar um plano liga os módulos do conjunto e desliga os demais.
 */
public final class PlanModules {

    private PlanModules() {}

    public static final String ESSENCIAL = "ESSENCIAL";
    public static final String PROFISSIONAL = "PROFISSIONAL";
    public static final String PREMIUM = "PREMIUM";

    /** Todos os módulos geridos por plano (o que pode ser ligado/desligado). */
    public static final Set<String> ALL = Set.of(
            "DASHBOARD", "CLIENTES", "EMPREENDIMENTOS", "VENDAS", "CONTAS_PAGAR",
            "CONTAS_RECEBER", "FORNECEDORES", "CONCILIACAO", "DRE", "RELATORIOS",
            "NOTIFICACOES", "PORTAL_CLIENTE", "APP_MOBILE");

    private static final Set<String> ESSENCIAL_SET = Set.of(
            "DASHBOARD", "CLIENTES", "FORNECEDORES", "CONTAS_PAGAR", "CONTAS_RECEBER",
            "CONCILIACAO", "DRE", "RELATORIOS", "NOTIFICACOES");

    private static final Set<String> PROFISSIONAL_SET = union(ESSENCIAL_SET,
            Set.of("EMPREENDIMENTOS", "VENDAS"));

    // PREMIUM acrescenta os módulos de roadmap (ainda a construir na Fase 4).
    private static final Set<String> PREMIUM_SET = union(PROFISSIONAL_SET,
            Set.of("PORTAL_CLIENTE", "APP_MOBILE"));

    private static final Map<String, Set<String>> BY_PLAN = Map.of(
            ESSENCIAL, ESSENCIAL_SET,
            PROFISSIONAL, PROFISSIONAL_SET,
            PREMIUM, PREMIUM_SET);

    public static final List<String> PLANS = List.of(ESSENCIAL, PROFISSIONAL, PREMIUM);

    /** Módulos do plano; vazio (nada liberado) se o plano for desconhecido. */
    public static Set<String> modulesFor(String plan) {
        return BY_PLAN.getOrDefault(plan == null ? "" : plan.toUpperCase(), Set.of());
    }

    public static boolean isValidPlan(String plan) {
        return plan != null && BY_PLAN.containsKey(plan.toUpperCase());
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        var s = new java.util.HashSet<>(a);
        s.addAll(b);
        return Set.copyOf(s);
    }
}
