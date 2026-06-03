package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.sale.SaleRequest;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a geração automática de parcelas e a regra da entrada.
 * Garante que a soma das parcelas sempre fecha exatamente o valor financiado.
 */
class SaleServiceInstallmentsTest {

    private PropertySale sale(String total, String down, int count, LocalDate firstDue) {
        PropertySale s = new PropertySale();
        s.setTotalValue(new BigDecimal(total));
        s.setDownPayment(new BigDecimal(down));
        s.setInstallmentsCount(count);
        s.setFirstDueDate(firstDue);
        s.setPaymentMethod("Boleto");
        return s;
    }

    private BigDecimal sum(List<Installment> list) {
        return list.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void divisaoExataFechaOTotal() {
        PropertySale s = sale("12000.00", "0", 12, LocalDate.of(2026, 7, 10));

        SaleService.generateInstallments(s);

        assertThat(s.getInstallments()).hasSize(12);
        assertThat(s.getInstallments()).allSatisfy(i -> assertThat(i.getAmount()).isEqualByComparingTo("1000.00"));
        assertThat(sum(s.getInstallments())).isEqualByComparingTo("12000.00");
    }

    @Test
    void residuoVaiParaUltimaParcela() {
        // 1000 / 3 = 333,33 (DOWN); resíduo 0,01 soma na última → 333,34
        PropertySale s = sale("1000.00", "0", 3, LocalDate.of(2026, 1, 15));

        SaleService.generateInstallments(s);

        List<Installment> p = s.getInstallments();
        assertThat(p).hasSize(3);
        assertThat(p.get(0).getAmount()).isEqualByComparingTo("333.33");
        assertThat(p.get(1).getAmount()).isEqualByComparingTo("333.33");
        assertThat(p.get(2).getAmount()).isEqualByComparingTo("333.34");
        assertThat(sum(p)).isEqualByComparingTo("1000.00"); // fecha exatamente
    }

    @Test
    void entradaReduzOValorFinanciado() {
        // (10000 - 1000) / 10 = 900 cada
        PropertySale s = sale("10000.00", "1000.00", 10, LocalDate.of(2026, 3, 1));

        SaleService.generateInstallments(s);

        assertThat(s.getInstallments()).hasSize(10);
        assertThat(sum(s.getInstallments())).isEqualByComparingTo("9000.00");
        assertThat(s.getInstallments()).allSatisfy(i -> assertThat(i.getAmount()).isEqualByComparingTo("900.00"));
    }

    @Test
    void vencimentosSaoMensaisAPartirDoPrimeiro() {
        PropertySale s = sale("3000.00", "0", 3, LocalDate.of(2026, 1, 31));

        SaleService.generateInstallments(s);

        assertThat(s.getInstallments().get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(s.getInstallments().get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 28)); // fevereiro
        assertThat(s.getInstallments().get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void numeracaoSequencialDasParcelas() {
        PropertySale s = sale("5000.00", "0", 5, LocalDate.of(2026, 1, 10));

        SaleService.generateInstallments(s);

        assertThat(s.getInstallments()).extracting(Installment::getNumber).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void semParcelasQuandoQuantidadeZero() {
        PropertySale s = sale("5000.00", "0", 0, LocalDate.of(2026, 1, 10));

        SaleService.generateInstallments(s);

        assertThat(s.getInstallments()).isEmpty();
    }

    // ---- regra da entrada (resolveDownPayment) ----

    private SaleRequest request(String purchaseType, String down) {
        return new SaleRequest(null, null, new BigDecimal("100000"),
                down != null ? new BigDecimal(down) : null,
                12, LocalDate.of(2026, 7, 1), purchaseType, "Boleto", "Sem correção",
                BigDecimal.ONE, new BigDecimal("2"), null);
    }

    @Test
    void entradaSoValeParaEntradaMaisParcelas() {
        assertThat(SaleService.resolveDownPayment(request("Entrada + parcelas", "50000")))
                .isEqualByComparingTo("50000");
    }

    @Test
    void entradaZeradaNasDemaisFormas() {
        assertThat(SaleService.resolveDownPayment(request("À vista", "50000")))
                .isEqualByComparingTo("0");
        assertThat(SaleService.resolveDownPayment(request("Financiamento próprio", "50000")))
                .isEqualByComparingTo("0");
    }
}
