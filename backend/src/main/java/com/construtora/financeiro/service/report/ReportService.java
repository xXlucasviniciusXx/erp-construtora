package com.construtora.financeiro.service.report;

import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.repository.*;
import com.construtora.financeiro.security.DevelopmentScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gera relatórios em CSV. Respeita o escopo por empreendimento (Fase E):
 * usuários restritos veem apenas os dados dos empreendimentos vinculados.
 * Relatórios que não se resolvem por empreendimento (conciliações, despesas por
 * categoria/centro de custo) vêm vazios para usuários restritos.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final AccountPayableRepository payableRepository;
    private final AccountReceivableRepository receivableRepository;
    private final InstallmentRepository installmentRepository;
    private final BankTransactionRepository transactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final PropertySaleRepository saleRepository;
    private final DevelopmentRepository developmentRepository;
    private final DevelopmentScopeService scope;

    public ReportService(AccountPayableRepository payableRepository,
                         AccountReceivableRepository receivableRepository,
                         InstallmentRepository installmentRepository,
                         BankTransactionRepository transactionRepository,
                         ReconciliationRepository reconciliationRepository,
                         PropertySaleRepository saleRepository,
                         DevelopmentRepository developmentRepository,
                         DevelopmentScopeService scope) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.installmentRepository = installmentRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.saleRepository = saleRepository;
        this.developmentRepository = developmentRepository;
        this.scope = scope;
    }

    private static UUID instDev(Installment i) {
        return i.getSale().getLot().getBlock().getDevelopment().getId();
    }

    /** Nomes de empreendimentos permitidos, ou {@code null} quando irrestrito. */
    private Set<String> allowedNames() {
        var ids = scope.allowedDevelopmentIds();
        if (ids.isEmpty()) return null;
        return developmentRepository.findAllById(ids.get()).stream()
                .map(Development::getName).collect(Collectors.toSet());
    }

    public String payableByPeriod(LocalDate start, LocalDate end) {
        List<List<Object>> rows = new ArrayList<>();
        scope.filter(payableRepository.findByDueDateBetween(start, end),
                        p -> p.getDevelopment() != null ? p.getDevelopment().getId() : null)
                .forEach(p -> rows.add(List.of(
                        p.getSupplier(),
                        nz(p.getCategory() != null ? p.getCategory().getName() : null),
                        nz(p.getCostCenter() != null ? p.getCostCenter().getName() : null),
                        nz(p.getDevelopment() != null ? p.getDevelopment().getName() : "Geral / Administrativo"),
                        nz(p.getDescription()), p.getAmount(),
                        p.getDueDate(), nz(p.getPaymentDate()), p.getStatus())));
        return CsvWriter.build(
                List.of("Fornecedor", "Categoria", "Centro de Custo", "Empreendimento", "Descrição", "Valor", "Vencimento", "Pagamento", "Status"), rows);
    }

    public String receivableByPeriod(LocalDate start, LocalDate end) {
        List<List<Object>> rows = new ArrayList<>();
        scope.filter(receivableRepository.findByDueDateBetween(start, end),
                        r -> r.getSale() != null ? r.getSale().getLot().getBlock().getDevelopment().getId() : null)
                .forEach(r -> rows.add(List.of(
                        r.getClient() != null ? r.getClient().getName() : "", nz(r.getDescription()), r.getAmount(),
                        r.getDueDate(), nz(r.getReceiveDate()), r.getStatus())));
        return CsvWriter.build(
                List.of("Cliente", "Descrição", "Valor", "Vencimento", "Recebimento", "Status"), rows);
    }

    public String overdueInstallments() {
        List<List<Object>> rows = new ArrayList<>();
        scope.filter(installmentRepository.findOverdueUnpaid(LocalDate.now()), ReportService::instDev)
                .forEach(i -> rows.add(List.of(
                        i.getSale().getClient().getName(), i.getNumber(), i.getAmount(), i.getDueDate(), i.getStatus())));
        return CsvWriter.build(List.of("Cliente", "Parcela", "Valor", "Vencimento", "Status"), rows);
    }

    public String reconciliations() {
        List<List<Object>> rows = new ArrayList<>();
        if (!scope.isRestricted()) {
            reconciliationRepository.findByUndoneFalseOrderByReconciledAtDesc().forEach(r -> rows.add(List.of(
                    r.getBankTransaction().getTransactionDate(), r.getBankTransaction().getDescription(),
                    r.getMatchedAmount(), r.getTargetType(), r.getMode(), r.getReconciledAt())));
        }
        return CsvWriter.build(
                List.of("Data", "Descrição", "Valor", "Alvo", "Modo", "Conciliado em"), rows);
    }

    public String pendingTransactions() {
        List<List<Object>> rows = new ArrayList<>();
        scope.filter(transactionRepository.findByStatus(com.construtora.financeiro.model.enums.TransactionStatus.PENDING),
                        t -> t.getBankAccount() != null && t.getBankAccount().getDevelopment() != null
                                ? t.getBankAccount().getDevelopment().getId() : null)
                .forEach(t -> rows.add(List.of(
                        t.getTransactionDate(), nz(t.getDescription()), t.getAmount(), t.getType(),
                        nz(t.getDocumentNumber()))));
        return CsvWriter.build(List.of("Data", "Descrição", "Valor", "Tipo", "Documento"), rows);
    }

    public String salesByDevelopment() {
        Set<String> names = allowedNames();
        List<List<Object>> rows = new ArrayList<>();
        saleRepository.salesByDevelopment().forEach(o -> {
            if (names == null || names.contains(o[0])) rows.add(List.of(o[0], o[1], o[2]));
        });
        return CsvWriter.build(List.of("Empreendimento", "Qtd. Vendas", "Valor Total"), rows);
    }

    public String expensesByDevelopment() {
        Set<String> names = allowedNames();
        List<List<Object>> rows = new ArrayList<>();
        payableRepository.expensesByDevelopmentPaid().forEach(o -> {
            // o[0] = nome do empreendimento (ou nulo = "Geral"); restritos só veem os seus
            if (names == null || (o[0] != null && names.contains(o[0]))) rows.add(List.of(nz(o[0]), nz(o[1]), nz(o[2])));
        });
        return CsvWriter.build(List.of("Empreendimento", "Qtd. Despesas Pagas", "Valor Total"), rows);
    }

    public String expensesByCategory() {
        List<List<Object>> rows = new ArrayList<>();
        if (!scope.isRestricted()) {
            payableRepository.expensesByCategoryPaid().forEach(o -> rows.add(List.of(nz(o[0]), nz(o[1]), nz(o[2]), nz(o[3]))));
        }
        return CsvWriter.build(List.of("Grupo", "Categoria", "Qtd. Despesas Pagas", "Valor Total"), rows);
    }

    public String expensesByCostCenter() {
        List<List<Object>> rows = new ArrayList<>();
        if (!scope.isRestricted()) {
            payableRepository.expensesByCostCenterPaid().forEach(o -> rows.add(List.of(nz(o[0]), nz(o[1]), nz(o[2]))));
        }
        return CsvWriter.build(List.of("Centro de Custo", "Qtd. Despesas Pagas", "Valor Total"), rows);
    }

    public String delinquentClients() {
        List<List<Object>> rows = new ArrayList<>();
        for (Installment i : scope.filter(installmentRepository.findOverdueUnpaid(LocalDate.now()), ReportService::instDev)) {
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
