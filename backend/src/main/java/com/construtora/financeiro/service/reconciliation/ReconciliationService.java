package com.construtora.financeiro.service.reconciliation;

import com.construtora.financeiro.dto.bank.BankTransactionResponse;
import com.construtora.financeiro.dto.reconciliation.ManualTargetResponse;
import com.construtora.financeiro.dto.reconciliation.ReconcileRequest;
import com.construtora.financeiro.dto.reconciliation.ReconciliationResponse;
import com.construtora.financeiro.dto.reconciliation.SuggestionResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.BankTransactionMapper;
import com.construtora.financeiro.model.*;
import com.construtora.financeiro.model.enums.*;
import com.construtora.financeiro.repository.*;
import com.construtora.financeiro.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra a conciliação bancária: gera/persiste sugestões, concilia
 * (manual ou aceitando sugestão), desfaz e lista pendências e histórico.
 */
@Service
@Transactional
public class ReconciliationService {

    private final BankTransactionRepository transactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationSuggestionRepository suggestionRepository;
    private final AccountReceivableRepository receivableRepository;
    private final AccountPayableRepository payableRepository;
    private final InstallmentRepository installmentRepository;
    private final SuggestionEngine suggestionEngine;
    private final BankTransactionMapper transactionMapper;
    private final com.construtora.financeiro.service.LateFeeCalculator lateFeeCalculator;

    public ReconciliationService(BankTransactionRepository transactionRepository,
                                 ReconciliationRepository reconciliationRepository,
                                 ReconciliationSuggestionRepository suggestionRepository,
                                 AccountReceivableRepository receivableRepository,
                                 AccountPayableRepository payableRepository,
                                 InstallmentRepository installmentRepository,
                                 SuggestionEngine suggestionEngine,
                                 BankTransactionMapper transactionMapper,
                                 com.construtora.financeiro.service.LateFeeCalculator lateFeeCalculator) {
        this.transactionRepository = transactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.suggestionRepository = suggestionRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.installmentRepository = installmentRepository;
        this.suggestionEngine = suggestionEngine;
        this.transactionMapper = transactionMapper;
        this.lateFeeCalculator = lateFeeCalculator;
    }

    @Transactional(readOnly = true)
    public List<BankTransactionResponse> pendencies() {
        return transactionRepository.findByStatus(TransactionStatus.PENDING)
                .stream().map(transactionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponse> history() {
        return reconciliationRepository.findByUndoneFalseOrderByReconciledAtDesc()
                .stream().map(this::toResponse).toList();
    }

    /** Gera as sugestões para uma transação e as persiste (substitui as anteriores). */
    public List<SuggestionResponse> generateSuggestions(UUID transactionId) {
        BankTransaction txn = getTransaction(transactionId);
        List<SuggestionResponse> suggestions = suggestionEngine.suggest(txn);

        suggestionRepository.deleteByBankTransactionId(transactionId);
        for (SuggestionResponse s : suggestions) {
            ReconciliationSuggestion entity = new ReconciliationSuggestion();
            entity.setBankTransaction(txn);
            entity.setTargetType(s.targetType());
            entity.setTargetId(s.targetId());
            entity.setScore(s.score());
            entity.setReason(s.reason());
            suggestionRepository.save(entity);
        }
        return suggestions;
    }

    /**
     * Lista todos os lançamentos em aberto compatíveis com o tipo da transação,
     * para o seletor de conciliação MANUAL (sem filtrar por valor exato).
     */
    @Transactional(readOnly = true)
    public List<ManualTargetResponse> manualTargets(UUID transactionId) {
        BankTransaction txn = getTransaction(transactionId);
        List<ManualTargetResponse> targets = new java.util.ArrayList<>();
        if (txn.getType() == TransactionType.CREDIT) {
            for (AccountReceivable r : receivableRepository.findByStatus(ReceivableStatus.OPEN)) {
                String client = r.getClient() != null ? r.getClient().getName() : "—";
                targets.add(new ManualTargetResponse(TargetType.RECEIVABLE, r.getId(),
                        "Receber: " + client + (r.getDescription() != null ? " (" + r.getDescription() + ")" : ""),
                        r.getAmount(), r.getDueDate()));
            }
            for (Installment i : installmentRepository.findAllReconcilable()) {
                var fees = lateFeeCalculator.compute(i);
                String label = "Parcela " + i.getNumber() + " — " + i.getSale().getClient().getName();
                if (fees.daysLate() > 0) {
                    label += " (atrasada " + fees.daysLate() + "d, + encargos)";
                }
                targets.add(new ManualTargetResponse(TargetType.INSTALLMENT, i.getId(),
                        label, fees.updatedAmount(), i.getDueDate()));
            }
        } else {
            for (AccountPayable p : payableRepository.findByStatus(PayableStatus.OPEN)) {
                targets.add(new ManualTargetResponse(TargetType.PAYABLE, p.getId(),
                        "Pagar: " + p.getSupplier(), p.getAmount(), p.getDueDate()));
            }
        }
        return targets;
    }

    /** Concilia uma transação a um lançamento (alvo). Marca ambos como liquidados. */
    public ReconciliationResponse reconcile(UUID transactionId, ReconcileRequest request, ReconciliationMode mode) {
        BankTransaction txn = getTransaction(transactionId);
        if (txn.getStatus() == TransactionStatus.RECONCILED) {
            throw new BusinessException("Transação já conciliada");
        }
        settleTarget(request.targetType(), request.targetId(), txn);

        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setBankTransaction(txn);
        reconciliation.setTargetType(request.targetType());
        reconciliation.setTargetId(request.targetId());
        reconciliation.setMatchedAmount(txn.getAmount().abs());
        reconciliation.setMode(mode);
        reconciliation.setNotes(request.notes());
        SecurityUtils.currentUserId().ifPresent(reconciliation::setReconciledBy);
        Reconciliation saved = reconciliationRepository.save(reconciliation);

        txn.setStatus(TransactionStatus.RECONCILED);
        transactionRepository.save(txn);
        suggestionRepository.deleteByBankTransactionId(transactionId);

        return toResponse(saved);
    }

    /** Desfaz uma conciliação, revertendo transação e alvo aos estados em aberto. */
    public void undo(UUID reconciliationId) {
        Reconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Conciliação", reconciliationId));
        if (reconciliation.isUndone()) {
            throw new BusinessException("Conciliação já foi desfeita");
        }
        reconciliation.setUndone(true);
        reconciliation.setUndoneAt(OffsetDateTime.now());
        reconciliationRepository.save(reconciliation);

        reopenTarget(reconciliation.getTargetType(), reconciliation.getTargetId());

        BankTransaction txn = reconciliation.getBankTransaction();
        txn.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(txn);
    }

    /** Marca a transação com um status manual (IGNORED, DIVERGENT, PENDING), com motivo opcional. */
    public BankTransactionResponse updateTransactionStatus(UUID transactionId, TransactionStatus status, String notes) {
        if (status == TransactionStatus.RECONCILED) {
            throw new BusinessException("Use o endpoint de conciliação para marcar como conciliada");
        }
        BankTransaction txn = getTransaction(transactionId);
        txn.setStatus(status);
        txn.setNotes(notes);   // motivo da divergência (ou limpa ao voltar para pendente)
        return transactionMapper.toResponse(transactionRepository.save(txn));
    }

    // ---- helpers ----

    private void settleTarget(TargetType type, UUID id, BankTransaction txn) {
        LocalDate date = txn.getTransactionDate();
        switch (type) {
            case RECEIVABLE -> {
                AccountReceivable r = receivableRepository.findById(id)
                        .orElseThrow(() -> ResourceNotFoundException.of("Conta a receber", id));
                r.setStatus(ReceivableStatus.RECEIVED);
                r.setReceiveDate(date);
                receivableRepository.save(r);
            }
            case PAYABLE -> {
                AccountPayable p = payableRepository.findById(id)
                        .orElseThrow(() -> ResourceNotFoundException.of("Conta a pagar", id));
                p.setStatus(PayableStatus.PAID);
                p.setPaymentDate(date);
                payableRepository.save(p);
            }
            case INSTALLMENT -> {
                Installment i = installmentRepository.findById(id)
                        .orElseThrow(() -> ResourceNotFoundException.of("Parcela", id));
                i.setStatus(InstallmentStatus.PAID);
                i.setPaymentDate(date);
                installmentRepository.save(i);
            }
        }
    }

    private void reopenTarget(TargetType type, UUID id) {
        switch (type) {
            case RECEIVABLE -> receivableRepository.findById(id).ifPresent(r -> {
                r.setStatus(ReceivableStatus.OPEN);
                r.setReceiveDate(null);
                receivableRepository.save(r);
            });
            case PAYABLE -> payableRepository.findById(id).ifPresent(p -> {
                p.setStatus(PayableStatus.OPEN);
                p.setPaymentDate(null);
                payableRepository.save(p);
            });
            case INSTALLMENT -> installmentRepository.findById(id).ifPresent(i -> {
                i.setStatus(InstallmentStatus.OPEN);
                i.setPaymentDate(null);
                installmentRepository.save(i);
            });
        }
    }

    private BankTransaction getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transação bancária", id));
    }

    private ReconciliationResponse toResponse(Reconciliation r) {
        return new ReconciliationResponse(
                r.getId(), r.getBankTransaction().getId(), r.getTargetType(), r.getTargetId(),
                r.getMatchedAmount(), r.getMode(), r.getConfidence(), r.getReconciledAt(),
                r.isUndone(), r.getNotes());
    }
}
