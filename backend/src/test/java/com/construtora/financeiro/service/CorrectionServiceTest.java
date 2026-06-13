package com.construtora.financeiro.service;

import com.construtora.financeiro.model.CorrectionIndex;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.repository.CorrectionIndexRepository;
import com.construtora.financeiro.service.CorrectionService.CorrectionResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Correção monetária pela regra de aniversário anual (índice oficial do BCB).
 * Lógica financeira sensível: o fator do BCB é mockado para isolar o cálculo.
 */
class CorrectionServiceTest {

    private final CorrectionIndexRepository indexRepo = mock(CorrectionIndexRepository.class);
    private final BcbIndexService bcb = mock(BcbIndexService.class);
    private final CorrectionService service = new CorrectionService(indexRepo, bcb);

    private CorrectionIndex incc() {
        CorrectionIndex c = new CorrectionIndex();
        c.setName("INCC");
        c.setSgsCode(192);
        c.setActive(true);
        return c;
    }

    private Installment installment(String amount, String index, LocalDate saleDate) {
        PropertySale s = new PropertySale();
        s.setCorrectionIndex(index);
        s.setSaleDate(saleDate);
        Installment i = new Installment();
        i.setAmount(new BigDecimal(amount));
        i.setSale(s);
        return i;
    }

    @Test
    void aniversariosAnuaisCompletos() {
        LocalDate base = LocalDate.of(2022, 1, 10);
        assertThat(CorrectionService.anniversaries(base, LocalDate.of(2022, 12, 31))).isZero();
        assertThat(CorrectionService.anniversaries(base, LocalDate.of(2023, 1, 10))).isEqualTo(1);
        assertThat(CorrectionService.anniversaries(base, LocalDate.of(2025, 6, 1))).isEqualTo(3);
    }

    @Test
    void corrige_pelo_fator_acumulado_dos_aniversarios() {
        LocalDate base = LocalDate.now().minusYears(3).minusMonths(1);   // 3 aniversários completos
        when(indexRepo.findByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(incc()));
        when(bcb.accumulatedFactorBetween(eq(192), any(), any())).thenReturn(1.30);   // +30% no período

        CorrectionResult r = service.correctedFor(installment("1000.00", "INCC", base));

        assertThat(r.anniversaries()).isEqualTo(3);
        assertThat(r.available()).isTrue();
        assertThat(r.correctedAmount()).isEqualByComparingTo("1300.00");
        assertThat(r.monetaryCorrection()).isEqualByComparingTo("300.00");
    }

    @Test
    void semIndiceOficial_naoCorrige() {
        when(indexRepo.findByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(incc()));
        CorrectionResult r = service.correctedFor(
                installment("1000.00", "Sem correção", LocalDate.now().minusYears(2)));

        assertThat(r.available()).isFalse();
        assertThat(r.correctedAmount()).isEqualByComparingTo("1000.00");
        assertThat(r.monetaryCorrection()).isEqualByComparingTo("0");
    }

    @Test
    void contratoComMenosDeUmAno_naoCorrige() {
        when(indexRepo.findByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(incc()));
        CorrectionResult r = service.correctedFor(
                installment("1000.00", "INCC", LocalDate.now().minusMonths(6)));

        assertThat(r.anniversaries()).isZero();
        assertThat(r.available()).isFalse();
        assertThat(r.correctedAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void bcbIndisponivel_naoCorrige() {
        LocalDate base = LocalDate.now().minusYears(2).minusMonths(1);
        when(indexRepo.findByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(incc()));
        when(bcb.accumulatedFactorBetween(eq(192), any(), any())).thenReturn(null);   // BCB fora do ar

        CorrectionResult r = service.correctedFor(installment("1000.00", "INCC", base));

        assertThat(r.available()).isFalse();
        assertThat(r.correctedAmount()).isEqualByComparingTo("1000.00");
        assertThat(r.monetaryCorrection()).isEqualByComparingTo("0");
    }
}
