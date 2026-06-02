package com.construtora.financeiro.service;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcula encargos por atraso (multa + juros de mora) de uma parcela, em tempo real.
 *
 * <p>Modelo padrão de mercado:
 * <ul>
 *   <li><b>Multa</b>: percentual fixo aplicado uma única vez sobre o valor da parcela
 *       ({@code penaltyRate} da venda).</li>
 *   <li><b>Juros de mora</b>: percentual ao mês, proporcional aos dias de atraso
 *       ("pro rata die") — {@code interestRate} da venda ÷ 30 × dias.</li>
 * </ul>
 *
 * <p>Os percentuais vêm de cada venda; se a venda tiver 0%, não há encargo.
 * Parcelas pagas ou canceladas não geram encargo.
 */
@Component
public class LateFeeCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DAYS_IN_MONTH = BigDecimal.valueOf(30);

    /** Resultado do cálculo de encargos de uma parcela em uma data de referência. */
    public record LateFees(long daysLate, BigDecimal penaltyAmount, BigDecimal interestAmount,
                           BigDecimal updatedAmount) {}

    /** Calcula os encargos da parcela na data de hoje. */
    public LateFees compute(Installment installment) {
        return compute(installment, LocalDate.now());
    }

    /** Calcula os encargos da parcela em uma data de referência. */
    public LateFees compute(Installment installment, LocalDate reference) {
        BigDecimal base = installment.getAmount();
        InstallmentStatus status = installment.getStatus();

        // Sem encargo: já paga, cancelada, ou ainda não vencida.
        if (status == InstallmentStatus.PAID || status == InstallmentStatus.CANCELLED) {
            return noFees(base);
        }
        long daysLate = ChronoUnit.DAYS.between(installment.getDueDate(), reference);
        if (daysLate <= 0) {
            return noFees(base);
        }

        PropertySale sale = installment.getSale();
        BigDecimal penaltyRate = orZero(sale.getPenaltyRate());   // % fixo
        BigDecimal interestRate = orZero(sale.getInterestRate());  // % ao mês

        BigDecimal penalty = base.multiply(penaltyRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal interest = base.multiply(interestRate)
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysLate))
                .divide(DAYS_IN_MONTH, 2, RoundingMode.HALF_UP);

        BigDecimal updated = base.add(penalty).add(interest);
        return new LateFees(daysLate, penalty, interest, updated);
    }

    private LateFees noFees(BigDecimal base) {
        return new LateFees(0L, BigDecimal.ZERO, BigDecimal.ZERO, base);
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
