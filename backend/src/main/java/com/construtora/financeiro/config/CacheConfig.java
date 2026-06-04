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

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                DASHBOARD_ANALYTICS, DASHBOARD_SUMMARY);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(200));
        return manager;
    }
}
