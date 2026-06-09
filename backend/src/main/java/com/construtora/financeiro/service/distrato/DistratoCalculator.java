package com.construtora.financeiro.service.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialOutcome;
import com.construtora.financeiro.model.enums.DistratoFinancialRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo financeiro puro do distrato (sem efeitos colaterais), isolado para
 * testes unitários determinísticos.
 *
 * <pre>
 * Retenção        = Valor Pago × Percentual / 100
 * Saldo Final     = Valor Pago − Retenção − Descontos (conforme a regra)
 *   Regra A (APENAS_RETENCAO)         → Descontos = 0
 *   Regra B (+ PARCELAS_VENCIDAS)     → Descontos = parcelas vencidas
 *   Regra C (+ VENCIDAS_E_ENCARGOS)   → Descontos = parcelas vencidas + encargos
 *   Regra D (+ SALDO_DEVEDOR_TOTAL)   → Descontos = saldo devedor total
 *
 * Saldo Final &gt; 0 → PAYABLE   (empresa devolve ao cliente)
 * Saldo Final &lt; 0 → RECEIVABLE (cliente ainda deve à empresa)
 * Saldo Final = 0 → ZERO       (sem lançamento financeiro)
 * </pre>
 */
public final class DistratoCalculator {

    private DistratoCalculator() {}

    public record DistratoCalculation(
            BigDecimal paidAmount,
            BigDecimal retentionPercent,
            BigDecimal retentionAmount,
            BigDecimal overdueAmount,
            BigDecimal chargesAmount,
            BigDecimal totalDebtAmount,
            BigDecimal deductions,            // descontos além da retenção (depende da regra)
            BigDecimal finalBalance,          // pode ser negativo
            DistratoFinancialOutcome outcome,
            BigDecimal financialEntryAmount   // |finalBalance| (valor do lançamento AP/AR); 0 se ZERO
    ) {}

    public static DistratoCalculation calculate(DistratoFinancialRule rule,
                                                BigDecimal paidAmount,
                                                BigDecimal retentionPercent,
                                                BigDecimal overdueAmount,
                                                BigDecimal chargesAmount,
                                                BigDecimal totalDebtAmount) {
        BigDecimal paid = nz(paidAmount);
        BigDecimal pct = nz(retentionPercent);
        BigDecimal overdue = nz(overdueAmount);
        BigDecimal charges = nz(chargesAmount);
        BigDecimal totalDebt = nz(totalDebtAmount);

        BigDecimal retention = paid.multiply(pct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal deductions = switch (rule) {
            case APENAS_RETENCAO_SOBRE_VALOR_PAGO -> BigDecimal.ZERO;
            case RETENCAO_MAIS_PARCELAS_VENCIDAS -> overdue;
            case RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS -> overdue.add(charges);
            case RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL -> totalDebt;
        };
        deductions = deductions.setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalBalance = paid.subtract(retention).subtract(deductions)
                .setScale(2, RoundingMode.HALF_UP);

        int sign = finalBalance.compareTo(BigDecimal.ZERO);
        DistratoFinancialOutcome outcome = sign > 0 ? DistratoFinancialOutcome.PAYABLE
                : sign < 0 ? DistratoFinancialOutcome.RECEIVABLE
                : DistratoFinancialOutcome.ZERO;

        BigDecimal entry = sign == 0 ? BigDecimal.ZERO.setScale(2)
                : finalBalance.abs().setScale(2, RoundingMode.HALF_UP);

        return new DistratoCalculation(
                paid.setScale(2, RoundingMode.HALF_UP),
                pct,
                retention,
                overdue.setScale(2, RoundingMode.HALF_UP),
                charges.setScale(2, RoundingMode.HALF_UP),
                totalDebt.setScale(2, RoundingMode.HALF_UP),
                deductions,
                finalBalance,
                outcome,
                entry);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
