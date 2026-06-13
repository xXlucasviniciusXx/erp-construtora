package com.construtora.financeiro.service;

import com.construtora.financeiro.model.CorrectionIndex;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.repository.CorrectionIndexRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Correção monetária das parcelas pela regra de <b>aniversário anual</b>: a cada
 * 12 meses completos a partir da data do contrato (saleDate), o saldo das
 * parcelas não pagas é corrigido pelo índice (INCC/IGP-M/IPCA) acumulado no
 * período, usando a série oficial do BCB (SGS).
 *
 * <p>Esta v1 <b>calcula e expõe</b> o valor corrigido (sem alterar o encargo
 * cobrado na baixa). Índices sem código SGS ("Sem correção", "Juros fixo
 * mensal", "Outro") ou BCB indisponível ⇒ sem correção.
 */
@Service
public class CorrectionService {

    private final CorrectionIndexRepository indexRepository;
    private final BcbIndexService bcb;

    public CorrectionService(CorrectionIndexRepository indexRepository, BcbIndexService bcb) {
        this.indexRepository = indexRepository;
        this.bcb = bcb;
    }

    /**
     * @param correctedAmount    valor corrigido até o último aniversário
     * @param monetaryCorrection delta de correção (correctedAmount − amount)
     * @param index              nome do índice aplicado
     * @param anniversaries      aniversários anuais completos considerados
     * @param available          {@code false} se índice sem SGS ou BCB indisponível
     */
    public record CorrectionResult(BigDecimal correctedAmount, BigDecimal monetaryCorrection,
                                   String index, int anniversaries, boolean available) {}

    /** Aniversários anuais completos entre {@code base} e {@code reference}. */
    public static int anniversaries(LocalDate base, LocalDate reference) {
        if (base == null || reference == null || !reference.isAfter(base)) return 0;
        return (int) Math.max(0, ChronoUnit.YEARS.between(base, reference));
    }

    /** Correção da parcela até hoje (parcelas pagas não são corrigidas). */
    public CorrectionResult correctedFor(Installment i) {
        BigDecimal amount = i.getAmount();
        PropertySale sale = i.getSale();
        String indexName = sale.getCorrectionIndex();
        LocalDate base = sale.getSaleDate();

        Integer sgs = sgsCodeOf(indexName);
        int n = anniversaries(base, LocalDate.now());
        if (sgs == null || n == 0) {
            return new CorrectionResult(amount, BigDecimal.ZERO, indexName, n, false);
        }
        Double factor = bcb.accumulatedFactorBetween(sgs, base, base.plusYears(n));
        if (factor == null) {   // BCB indisponível
            return new CorrectionResult(amount, BigDecimal.ZERO, indexName, n, false);
        }
        BigDecimal corrected = amount.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        return new CorrectionResult(corrected, corrected.subtract(amount), indexName, n, true);
    }

    /** Código SGS do índice (por nome), ou {@code null} se não tiver fonte oficial. */
    @Cacheable(value = "correctionSgs", key = "#name == null ? '' : #name.toLowerCase()")
    public Integer sgsCodeOf(String name) {
        if (name == null || name.isBlank()) return null;
        return indexRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .filter(c -> name.equalsIgnoreCase(c.getName()) && c.getSgsCode() != null)
                .map(CorrectionIndex::getSgsCode)
                .findFirst().orElse(null);
    }
}
