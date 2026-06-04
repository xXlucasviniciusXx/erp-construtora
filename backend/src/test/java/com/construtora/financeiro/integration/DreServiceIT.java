package com.construtora.financeiro.integration;

import com.construtora.financeiro.dto.dre.DreResponse;
import com.construtora.financeiro.service.DreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que o DreService executa sem erros contra PostgreSQL real e que
 * a estrutura da resposta é consistente em ambas as bases.
 *
 * <p>Os dados de demonstração (V3/V5) são usados; os testes verificam
 * invariâncias (resultado = receitas − despesas) e não valores absolutos.
 */
@Transactional
class DreServiceIT extends AbstractIntegrationTest {

    @Autowired
    DreService dreService;

    @Test
    void dreCaixa_resultIsConsistent() {
        DreResponse dre = dreService.dre(null, null, null, DreService.Basis.CAIXA);

        assertThat(dre).isNotNull();
        assertThat(dre.basis()).isEqualTo("CAIXA");
        assertThat(dre.revenues()).isNotEmpty();
        assertThat(dre.expenses()).isNotNull();
        // invariância: resultado = receitas - despesas (com tolerância float)
        assertThat(dre.result())
                .isCloseTo(dre.totalRevenue() - dre.totalExpense(), org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void dreCompetencia_resultIsConsistent() {
        DreResponse dre = dreService.dre(null, null, null, DreService.Basis.COMPETENCIA);

        assertThat(dre).isNotNull();
        assertThat(dre.basis()).isEqualTo("COMPETENCIA");
        assertThat(dre.revenues()).isNotEmpty();
        assertThat(dre.result())
                .isCloseTo(dre.totalRevenue() - dre.totalExpense(), org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void dreCaixa_defaultBasis() {
        // sem parâmetro de base → deve usar CAIXA
        DreResponse dre = dreService.dre(null, null, null, null);
        assertThat(dre.basis()).isEqualTo("CAIXA");
    }

    @Test
    void dreWithDateRange_doesNotThrow() {
        // filtro de período não deve lançar exceção, mesmo sem dados no intervalo
        java.time.LocalDate from = java.time.LocalDate.of(2030, 1, 1);
        java.time.LocalDate to   = java.time.LocalDate.of(2030, 12, 31);
        DreResponse dre = dreService.dre(from, to, null, DreService.Basis.CAIXA);

        assertThat(dre).isNotNull();
        assertThat(dre.totalRevenue()).isZero();
        assertThat(dre.totalExpense()).isZero();
        assertThat(dre.result()).isZero();
    }
}
