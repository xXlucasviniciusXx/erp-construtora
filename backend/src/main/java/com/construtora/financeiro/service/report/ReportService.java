package com.construtora.financeiro.service.report;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Gera relatórios em CSV. Cada método devolve o conteúdo pronto para download. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final AccountPayableRepository payableRepository;
    private final AccountReceivableRepository receivableRepository;
    private final InstallmentRepository installmentRepository;
    private final BankTransactionRepository transactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final PropertySaleRepository saleRepository;

    public ReportService(AccountPayableRepository payableRepository,
                         AccountReceivableRepository receivableRepository,
                         InstallmentRepository installmentRepository,
                         BankTransactionRepository transactionRepository,
                         ReconciliationRepository reconciliationRepository,
                         PropertySaleRepository saleRepository) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.installmentRepository = installmentRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.saleRepository = saleRepository;
    }

    public String payableByPeriod(LocalDate start, LocalDate end) {
        List<List<Object>> rows = new ArrayList<>();
        payableRepository.findByDueDateBetween(start, end).forEach(p -> rows.add(List.of(
                p.getSupplier(), nz(p.getCategory()), nz(p.getDescription()), p.getAmount(),
                p.getDueDate(), nz(p.getPaymentDate()), p.getStatus())));
        return CsvWriter.build(
                List.of("Fornecedor", "Categoria", "Descrição", "Valor", "Vencimento", "Pagamento", "Status"), rows);
    }

    public String receivableByPeriod(LocalDate start, LocalDate end) {
        List<List<Object>> rows = new ArrayList<>();
        receivableRepository.findByDueDateBetween(start, end).forEach(r -> rows.add(List.of(
                r.getClient() != null ? r.getClient().getName() : "", nz(r.getDescription()), r.getAmount(),
                r.getDueDate(), nz(r.getReceiveDate()), r.getStatus())));
        return CsvWriter.build(
                List.of("Cliente", "Descrição", "Valor", "Vencimento", "Recebimento", "Status"), rows);
    }

    public String overdueInstallments() {
        List<List<Object>> rows = new ArrayList<>();
        installmentRepository.findOverdueUnpaid(LocalDate.now()).forEach(i -> rows.add(List.of(
                i.getSale().getClient().getName(), i.getNumber(), i.getAmount(), i.getDueDate(), i.getStatus())));
        return CsvWriter.build(List.of("Cliente", "Parcela", "Valor", "Vencimento", "Status"), rows);
    }

    public String reconciliations() {
        List<List<Object>> rows = new ArrayList<>();
        reconciliationRepository.findByUndoneFalseOrderByReconciledAtDesc().forEach(r -> rows.add(List.of(
                r.getBankTransaction().getTransactionDate(), r.getBankTransaction().getDescription(),
                r.getMatchedAmount(), r.getTargetType(), r.getMode(), r.getReconciledAt())));
        return CsvWriter.build(
                List.of("Data", "Descrição", "Valor", "Alvo", "Modo", "Conciliado em"), rows);
    }

    public String pendingTransactions() {
        List<List<Object>> rows = new ArrayList<>();
        transactionRepository.findByStatus(com.construtora.financeiro.model.enums.TransactionStatus.PENDING)
                .forEach(t -> rows.add(List.of(
                        t.getTransactionDate(), nz(t.getDescription()), t.getAmount(), t.getType(),
                        nz(t.getDocumentNumber()))));
        return CsvWriter.build(List.of("Data", "Descrição", "Valor", "Tipo", "Documento"), rows);
    }

    public String salesByDevelopment() {
        List<List<Object>> rows = new ArrayList<>();
        saleRepository.salesByDevelopment().forEach(o -> rows.add(List.of(o[0], o[1], o[2])));
        return CsvWriter.build(List.of("Empreendimento", "Qtd. Vendas", "Valor Total"), rows);
    }

    public String expensesByDevelopment() {
        List<List<Object>> rows = new ArrayList<>();
        payableRepository.expensesByDevelopmentPaid().forEach(o -> rows.add(List.of(nz(o[0]), nz(o[1]), nz(o[2]))));
        return CsvWriter.build(List.of("Empreendimento", "Qtd. Despesas Pagas", "Valor Total"), rows);
    }

    public String delinquentClients() {
        List<List<Object>> rows = new ArrayList<>();
        for (Installment i : installmentRepository.findOverdueUnpaid(LocalDate.now())) {
            rows.add(List.of(
                    i.getSale().getClient().getName(), i.getSale().getClient().getDocument(),
                    i.getNumber(), i.getAmount(), i.getDueDate()));
        }
        return CsvWriter.build(
                List.of("Cliente", "Documento", "Parcela", "Valor", "Vencimento"), rows);
    }

    private Object nz(Object v) {
        return v == null ? "" : v;
    }
}
