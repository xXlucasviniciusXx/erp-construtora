package com.construtora.financeiro.service.reconciliation;

import com.construtora.financeiro.dto.reconciliation.SuggestionResponse;
import com.construtora.financeiro.model.*;
import com.construtora.financeiro.model.enums.TransactionType;
import com.construtora.financeiro.repository.AccountPayableRepository;
import com.construtora.financeiro.repository.AccountReceivableRepository;
import com.construtora.financeiro.repository.InstallmentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Valida o escopo por empreendimento na sugestão de conciliação (Fase D):
 * quando a conta bancária está vinculada a um empreendimento, apenas os
 * lançamentos daquele empreendimento (ou gerais) são sugeridos.
 */
class SuggestionEngineScopeTest {

    private final AccountReceivableRepository receivableRepo = mock(AccountReceivableRepository.class);
    private final AccountPayableRepository payableRepo = mock(AccountPayableRepository.class);
    private final InstallmentRepository installmentRepo = mock(InstallmentRepository.class);
    private final SuggestionEngine engine = new SuggestionEngine(receivableRepo, payableRepo, installmentRepo);

    private Development dev(String name) {
        Development d = new Development();
        d.setId(UUID.randomUUID());
        d.setName(name);
        return d;
    }

    private Installment installmentInDev(Development d, int number) {
        Block b = new Block(); b.setId(UUID.randomUUID()); b.setName("Q1"); b.setDevelopment(d);
        Lot lot = new Lot(); lot.setId(UUID.randomUUID()); lot.setName("L" + number); lot.setBlock(b);
        Client c = new Client(); c.setId(UUID.randomUUID()); c.setName("Cliente " + number); c.setDocument("000");
        PropertySale s = new PropertySale(); s.setId(UUID.randomUUID()); s.setClient(c); s.setLot(lot);
        Installment i = new Installment();
        i.setId(UUID.randomUUID());
        i.setNumber(number);
        i.setAmount(new BigDecimal("100.00"));
        i.setDueDate(LocalDate.of(2026, 1, 10));
        i.setSale(s);
        return i;
    }

    private BankTransaction credit(Development accountDev) {
        BankAccount acc = new BankAccount();
        acc.setId(UUID.randomUUID());
        acc.setName("Conta");
        acc.setDevelopment(accountDev);   // pode ser null (Geral)
        BankTransaction t = new BankTransaction();
        t.setId(UUID.randomUUID());
        t.setBankAccount(acc);
        t.setAmount(new BigDecimal("100.00"));
        t.setType(TransactionType.CREDIT);
        t.setTransactionDate(LocalDate.of(2026, 1, 10));
        t.setDescription("Credito recebido");
        return t;
    }

    @Test
    void contaVinculada_sugereApenasParcelasDoMesmoEmpreendimento() {
        Development d1 = dev("Empreendimento A");
        Development d2 = dev("Empreendimento B");
        Installment inA = installmentInDev(d1, 1);
        Installment inB = installmentInDev(d2, 2);
        when(installmentRepo.findReconcilableByAmount(any())).thenReturn(List.of(inA, inB));
        when(receivableRepo.findReconcilableByAmount(any())).thenReturn(List.of());

        List<SuggestionResponse> result = engine.suggest(credit(d1));

        assertThat(result).extracting(SuggestionResponse::targetId)
                .contains(inA.getId())
                .doesNotContain(inB.getId());
    }

    @Test
    void contaGeral_sugereParcelasDeQualquerEmpreendimento() {
        Development d1 = dev("Empreendimento A");
        Development d2 = dev("Empreendimento B");
        Installment inA = installmentInDev(d1, 1);
        Installment inB = installmentInDev(d2, 2);
        when(installmentRepo.findReconcilableByAmount(any())).thenReturn(List.of(inA, inB));
        when(receivableRepo.findReconcilableByAmount(any())).thenReturn(List.of());

        List<SuggestionResponse> result = engine.suggest(credit(null));   // conta sem empreendimento

        assertThat(result).extracting(SuggestionResponse::targetId)
                .contains(inA.getId(), inB.getId());
    }
}
