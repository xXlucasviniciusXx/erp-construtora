package com.construtora.financeiro.service;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import com.construtora.financeiro.service.LateFeeCalculator.LateFees;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o cálculo de encargos por atraso (multa fixa + juros de mora pro rata die).
 * Lógica financeira sensível: qualquer regressão aqui muda o valor cobrado do cliente.
 */
class LateFeeCalculatorTest {

    private final LateFeeCalculator calc = new LateFeeCalculator();

    private Installment installment(BigDecimal amount, InstallmentStatus status, LocalDate due,
                                    String penaltyRate, String interestRate) {
        PropertySale sale = new PropertySale();
        sale.setPenaltyRate(penaltyRate != null ? new BigDecimal(penaltyRate) : null);
        sale.setInterestRate(interestRate != null ? new BigDecimal(interestRate) : null);
        Installment i = new Installment();
        i.setAmount(amount);
        i.setStatus(status);
        i.setDueDate(due);
        i.setSale(sale);
        return i;
    }

    @Test
    void multaFixaMaisJurosProRataDie() {
        // Parcela de 1000, 45 dias de atraso, multa 2% + juros 1% a.m.
        LocalDate due = LocalDate.of(2026, 1, 1);
        Installment i = installment(new BigDecimal("1000.00"), InstallmentStatus.OVERDUE, due, "2", "1");

        LateFees f = calc.compute(i, due.plusDays(45));

        assertThat(f.daysLate()).isEqualTo(45);
        assertThat(f.penaltyAmount()).isEqualByComparingTo("20.00");           // 1000 * 2%
        assertThat(f.interestAmount()).isEqualByComparingTo("15.00");          // 1000 * 1% * 45/30
        assertThat(f.updatedAmount()).isEqualByComparingTo("1035.00");         // 1000 + 20 + 15
    }

    @Test
    void parcelaPagaNaoGeraEncargo() {
        Installment i = installment(new BigDecimal("1000.00"), InstallmentStatus.PAID,
                LocalDate.of(2026, 1, 1), "2", "1");

        LateFees f = calc.compute(i, LocalDate.of(2026, 6, 1));

        assertThat(f.daysLate()).isZero();
        assertThat(f.penaltyAmount()).isEqualByComparingTo("0");
        assertThat(f.interestAmount()).isEqualByComparingTo("0");
        assertThat(f.updatedAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void parcelaCanceladaNaoGeraEncargo() {
        Installment i = installment(new BigDecimal("500.00"), InstallmentStatus.CANCELLED,
                LocalDate.of(2026, 1, 1), "2", "1");

        LateFees f = calc.compute(i, LocalDate.of(2026, 6, 1));

        assertThat(f.daysLate()).isZero();
        assertThat(f.updatedAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void parcelaNoPrazoNaoGeraEncargo() {
        LocalDate due = LocalDate.of(2026, 12, 1);
        Installment i = installment(new BigDecimal("1000.00"), InstallmentStatus.OPEN, due, "2", "1");

        LateFees f = calc.compute(i, due.minusDays(10)); // ainda não venceu

        assertThat(f.daysLate()).isZero();
        assertThat(f.updatedAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void semTaxasNaVendaNaoCobraEncargoMasContaOAtraso() {
        // Atrasada, porém a venda tem juros/multa = 0 → sem encargo, valor inalterado.
        LocalDate due = LocalDate.of(2026, 1, 1);
        Installment i = installment(new BigDecimal("1000.00"), InstallmentStatus.OVERDUE, due, "0", "0");

        LateFees f = calc.compute(i, due.plusDays(30));

        assertThat(f.daysLate()).isEqualTo(30);
        assertThat(f.penaltyAmount()).isEqualByComparingTo("0");
        assertThat(f.interestAmount()).isEqualByComparingTo("0");
        assertThat(f.updatedAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void taxasNulasNaVendaSaoTratadasComoZero() {
        LocalDate due = LocalDate.of(2026, 1, 1);
        Installment i = installment(new BigDecimal("1000.00"), InstallmentStatus.OVERDUE, due, null, null);

        LateFees f = calc.compute(i, due.plusDays(30));

        assertThat(f.updatedAmount()).isEqualByComparingTo("1000.00");
    }
}
