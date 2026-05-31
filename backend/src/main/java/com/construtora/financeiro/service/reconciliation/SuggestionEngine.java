package com.construtora.financeiro.service.reconciliation;

import com.construtora.financeiro.dto.reconciliation.SuggestionResponse;
import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.AccountReceivable;
import com.construtora.financeiro.model.BankTransaction;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.enums.TargetType;
import com.construtora.financeiro.model.enums.TransactionType;
import com.construtora.financeiro.repository.AccountPayableRepository;
import com.construtora.financeiro.repository.AccountReceivableRepository;
import com.construtora.financeiro.repository.InstallmentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor de sugestão de conciliação. Para uma transação bancária, busca
 * lançamentos compatíveis e atribui um score (0–100) combinando:
 *   - valor exato            (peso base: 60)
 *   - proximidade da data    (até 25)
 *   - documento (CPF/CNPJ)   (15)
 *
 * Créditos são comparados a contas a receber e parcelas; débitos a contas a pagar.
 */
@Component
public class SuggestionEngine {

    private static final BigDecimal AMOUNT_WEIGHT = BigDecimal.valueOf(60);
    private static final BigDecimal DATE_WEIGHT = BigDecimal.valueOf(25);
    private static final BigDecimal DOC_WEIGHT = BigDecimal.valueOf(15);
    private static final int DATE_TOLERANCE_DAYS = 5;

    private final AccountReceivableRepository receivableRepository;
    private final AccountPayableRepository payableRepository;
    private final InstallmentRepository installmentRepository;

    public SuggestionEngine(AccountReceivableRepository receivableRepository,
                            AccountPayableRepository payableRepository,
                            InstallmentRepository installmentRepository) {
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.installmentRepository = installmentRepository;
    }

    public List<SuggestionResponse> suggest(BankTransaction txn) {
        BigDecimal absAmount = txn.getAmount().abs();
        List<SuggestionResponse> suggestions = new ArrayList<>();

        if (txn.getType() == TransactionType.CREDIT) {
            for (AccountReceivable r : receivableRepository.findReconcilableByAmount(absAmount)) {
                suggestions.add(score(TargetType.RECEIVABLE, r.getId(),
                        labelReceivable(r), r.getAmount(), r.getDueDate(),
                        clientDocument(r), txn));
            }
            for (Installment i : installmentRepository.findReconcilableByAmount(absAmount)) {
                suggestions.add(score(TargetType.INSTALLMENT, i.getId(),
                        labelInstallment(i), i.getAmount(), i.getDueDate(),
                        i.getSale().getClient().getDocument(), txn));
            }
        } else {
            for (AccountPayable p : payableRepository.findReconcilableByAmount(absAmount)) {
                suggestions.add(score(TargetType.PAYABLE, p.getId(),
                        "Pagar: " + p.getSupplier(), p.getAmount(), p.getDueDate(), null, txn));
            }
        }

        suggestions.sort(Comparator.comparing(SuggestionResponse::score).reversed());
        return suggestions;
    }

    private SuggestionResponse score(TargetType type, java.util.UUID id, String label,
                                     BigDecimal amount, LocalDate dueDate, String document,
                                     BankTransaction txn) {
        BigDecimal score = BigDecimal.ZERO;
        StringBuilder reason = new StringBuilder();

        // Valor (já filtrado por igualdade na query)
        score = score.add(AMOUNT_WEIGHT);
        reason.append("valor exato");

        // Data
        long days = dueDate != null
                ? Math.abs(ChronoUnit.DAYS.between(dueDate, txn.getTransactionDate())) : Long.MAX_VALUE;
        if (days <= DATE_TOLERANCE_DAYS) {
            BigDecimal proximity = DATE_WEIGHT
                    .multiply(BigDecimal.valueOf(DATE_TOLERANCE_DAYS - days))
                    .divide(BigDecimal.valueOf(DATE_TOLERANCE_DAYS), 2, java.math.RoundingMode.HALF_UP);
            score = score.add(proximity);
            reason.append("; data próxima (").append(days).append("d)");
        }

        // Documento
        if (document != null && txn.getDocumentNumber() != null
                && document.replaceAll("\\D", "").equals(txn.getDocumentNumber().replaceAll("\\D", ""))) {
            score = score.add(DOC_WEIGHT);
            reason.append("; documento confere");
        }

        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        return new SuggestionResponse(type, id, label, amount, dueDate, score, reason.toString());
    }

    private String labelReceivable(AccountReceivable r) {
        String client = r.getClient() != null ? r.getClient().getName() : "—";
        return "Receber: " + client + (r.getDescription() != null ? " (" + r.getDescription() + ")" : "");
    }

    private String labelInstallment(Installment i) {
        return "Parcela " + i.getNumber() + " — " + i.getSale().getClient().getName();
    }

    private String clientDocument(AccountReceivable r) {
        return r.getClient() != null ? r.getClient().getDocument() : null;
    }
}
