package com.construtora.financeiro.service.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialOutcome;
import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import com.construtora.financeiro.service.distrato.DistratoCalculator.DistratoCalculation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre os cenários de cálculo do distrato exigidos na especificação (Testes 1–5).
 * Lógica financeira sensível: qualquer regressão altera o valor devolvido/cobrado do cliente.
 */
class DistratoCalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void teste1_apenasRetencaoSobreValorPago() {
        DistratoCalculation r = DistratoCalculator.calculate(
                DistratoFinancialRule.APENAS_RETENCAO_SOBRE_VALOR_PAGO,
                bd("50000"), bd("20"), bd("0"), bd("0"), bd("0"));

        assertThat(r.retentionAmount()).isEqualByComparingTo("10000.00");
        assertThat(r.finalBalance()).isEqualByComparingTo("40000.00");
        assertThat(r.outcome()).isEqualTo(DistratoFinancialOutcome.PAYABLE);
        assertThat(r.financialEntryAmount()).isEqualByComparingTo("40000.00");
    }

    @Test
    void teste2_retencaoMaisParcelasVencidas() {
        DistratoCalculation r = DistratoCalculator.calculate(
                DistratoFinancialRule.RETENCAO_MAIS_PARCELAS_VENCIDAS,
                bd("50000"), bd("20"), bd("5000"), bd("0"), bd("0"));

        assertThat(r.retentionAmount()).isEqualByComparingTo("10000.00");
        assertThat(r.finalBalance()).isEqualByComparingTo("35000.00");
        assertThat(r.outcome()).isEqualTo(DistratoFinancialOutcome.PAYABLE);
        assertThat(r.financialEntryAmount()).isEqualByComparingTo("35000.00");
    }

    @Test
    void teste3_retencaoMaisParcelasVencidasEEncargos() {
        DistratoCalculation r = DistratoCalculator.calculate(
                DistratoFinancialRule.RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS,
                bd("50000"), bd("20"), bd("5000"), bd("1000"), bd("0"));

        assertThat(r.retentionAmount()).isEqualByComparingTo("10000.00");
        assertThat(r.finalBalance()).isEqualByComparingTo("34000.00");
        assertThat(r.outcome()).isEqualTo(DistratoFinancialOutcome.PAYABLE);
        assertThat(r.financialEntryAmount()).isEqualByComparingTo("34000.00");
    }

    @Test
    void teste4_saldoDevedorTotal_gerandoContasAReceber() {
        DistratoCalculation r = DistratoCalculator.calculate(
                DistratoFinancialRule.RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL,
                bd("20000"), bd("20"), bd("0"), bd("0"), bd("25000"));

        assertThat(r.retentionAmount()).isEqualByComparingTo("4000.00");
        assertThat(r.finalBalance()).isEqualByComparingTo("-9000.00");
        assertThat(r.outcome()).isEqualTo(DistratoFinancialOutcome.RECEIVABLE);
        assertThat(r.financialEntryAmount()).isEqualByComparingTo("9000.00");
    }

    @Test
    void teste5_saldoFinalZero_naoGeraFinanceiro() {
        // Retenção 2.000 + descontos 8.000 = 10.000 (= valor pago) → saldo zero.
        DistratoCalculation r = DistratoCalculator.calculate(
                DistratoFinancialRule.RETENCAO_MAIS_PARCELAS_VENCIDAS,
                bd("10000"), bd("20"), bd("8000"), bd("0"), bd("0"));

        assertThat(r.retentionAmount()).isEqualByComparingTo("2000.00");
        assertThat(r.finalBalance()).isEqualByComparingTo("0.00");
        assertThat(r.outcome()).isEqualTo(DistratoFinancialOutcome.ZERO);
        assertThat(r.financialEntryAmount()).isEqualByComparingTo("0.00");
    }
}
