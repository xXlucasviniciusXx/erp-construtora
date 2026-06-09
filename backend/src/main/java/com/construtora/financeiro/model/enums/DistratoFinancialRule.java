package com.construtora.financeiro.model.enums;

/**
 * Regra financeira aplicada no cálculo do distrato. Define quais descontos
 * incidem sobre o valor pago, além da retenção.
 */
public enum DistratoFinancialRule {
    /** Apenas retenção sobre o valor pago. Ignora parcelas. */
    APENAS_RETENCAO_SOBRE_VALOR_PAGO,
    /** Retenção + parcelas vencidas em aberto (padrão recomendado). */
    RETENCAO_MAIS_PARCELAS_VENCIDAS,
    /** Retenção + parcelas vencidas + encargos (multa/juros/correção). */
    RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS,
    /** Retenção + saldo devedor total (vencidas e vincendas). */
    RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL
}
