package com.construtora.financeiro.config;

import com.construtora.financeiro.security.LicenseEnforcementInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra o enforcement de licenciamento para todas as rotas de API. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LicenseEnforcementInterceptor licenseInterceptor;

    public WebConfig(LicenseEnforcementInterceptor licenseInterceptor) {
        this.licenseInterceptor = licenseInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenseInterceptor).addPathPatterns("/api/**");
    }
}
