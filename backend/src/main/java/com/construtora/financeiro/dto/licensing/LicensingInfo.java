package com.construtora.financeiro.dto.licensing;

import java.util.List;

/**
 * Bootstrap de licenciamento para o frontend: módulos (para montar o menu)
 * + licença (para avisos de vencimento). Servido a qualquer usuário logado.
 */
public record LicensingInfo(
        List<ModuleResponse> modules,
        LicenseResponse license
) {}
