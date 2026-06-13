package com.construtora.financeiro.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache em memória (Caffeine) para as agregações pesadas do dashboard/DRE.
 *
 * <p>TTL curto (60 s): o painel reflete mudanças quase em tempo real, mas evita
 * recalcular as ~20 queries nativas a cada refresh quando vários usuários estão
 * com o dashboard aberto. Tamanho máximo limitado para não crescer sem controle
 * com muitas combinações de filtros.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_ANALYTICS = "dashboardAnalytics";
    public static final String DASHBOARD_SUMMARY = "dashboardSummary";
    public static final String BCB_INDEX = "bcbIndex";
    public static final String BCB_INDEX_FACTOR = "bcbIndexFactor";
    public static final String CORRECTION_SGS = "correctionSgs";
    public static final String PUBLIC_SETTINGS = "publicSettings";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // Cache padrão (60s) para as agregações do dashboard
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(200));
        manager.registerCustomCache(DASHBOARD_ANALYTICS, defaultCache());
        manager.registerCustomCache(DASHBOARD_SUMMARY, defaultCache());
        // Índices do BCB mudam mensalmente — cache longo (6h)
        manager.registerCustomCache(BCB_INDEX, Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(50)
                .build());
        // Fator de correção acumulado por (sgs, período) — mesma natureza mensal (6h)
        manager.registerCustomCache(BCB_INDEX_FACTOR, Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(500)
                .build());
        // Mapa nome do índice → código SGS (muda raramente) — 1h
        manager.registerCustomCache(CORRECTION_SGS, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(50)
                .build());
        // Settings públicos: lidos em todo carregamento de página (branding),
        // mudam raramente. Cache de 10 min, invalidado nas escritas — evita reler
        // a linha (incluindo o BYTEA do logo) a cada requisição.
        manager.registerCustomCache(PUBLIC_SETTINGS, Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(4)
                .build());
        return manager;
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> defaultCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(200)
                .build();
    }
}
