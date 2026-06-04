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
        return manager;
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> defaultCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(200)
                .build();
    }
}
