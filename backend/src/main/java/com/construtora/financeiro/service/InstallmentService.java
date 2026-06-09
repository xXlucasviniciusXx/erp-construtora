package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.sale.InstallmentDetailResponse;
import com.construtora.financeiro.dto.sale.InstallmentPaymentRequest;
import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.SaleMapper;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import com.construtora.financeiro.repository.InstallmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InstallmentService {

    private final InstallmentRepository repository;
    private final NotificationService notificationService;
    private final com.construtora.financeiro.security.DevelopmentScopeService scope;
    private final SaleMapper mapper;

    public InstallmentService(InstallmentRepository repository,
                              NotificationService notificationService,
                              com.construtora.financeiro.security.DevelopmentScopeService scope, SaleMapper mapper) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.scope = scope;
        this.mapper = mapper;
    }

    /** Empreendimento da parcela (via venda → lote → quadra). */
    private static UUID devOf(Installment i) {
        return i.getSale().getLot().getBlock().getDevelopment().getId();
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> findBySale(UUID saleId) {
        return scope.filter(repository.findBySaleId(saleId), InstallmentService::devOf)
                .stream().map(mapper::toInstallmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> findOverdue() {
        return scope.filter(repository.findOverdueUnpaid(LocalDate.now()), InstallmentService::devOf)
                .stream().map(mapper::toInstallmentResponse).toList();
    }

    /** Lista parcelas (com dados do cliente) aplicando filtros opcionais, paginado. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<InstallmentDetailResponse> search(
            String q, InstallmentStatus status, LocalDate dueFrom, LocalDate dueTo,
            org.springframework.data.domain.Pageable pageable) {
        String query = (q != null && !q.isBlank()) ? q.trim() : "";
        var qs = scope.queryScope();
        return repository.search(query, status, dueFrom, dueTo, qs.unrestricted(), qs.devIds(), pageable)
                .map(mapper::toDetailResponse);
    }

    /** Confirma o pagamento de uma parcela e dispara a notificação de confirmação. */
    public InstallmentResponse confirmPayment(UUID id, InstallmentPaymentRequest request) {
        Installment inst = getEntity(id);
        if (inst.getStatus() == InstallmentStatus.PAID) {
            throw new BusinessException("Parcela já está paga");
        }
        inst.setStatus(InstallmentStatus.PAID);
        inst.setPaymentDate(request.paymentDate());
        if (request.paymentMethod() != null) inst.setPaymentMethod(request.paymentMethod());
        inst.setReceiptUrl(request.receiptUrl());
        if (request.notes() != null) inst.setNotes(request.notes());
        Installment saved = repository.save(inst);
        notificationService.notifyPaymentConfirmed(saved);
        return mapper.toInstallmentResponse(saved);
    }

    /**
     * Marca como atrasadas as parcelas em aberto cuja data de vencimento já passou.
     * Chamado pelo job agendado (ver {@code OverdueScheduler}).
     */
    public int markOverdue() {
        List<Installment> due = repository.findByStatusAndDueDateBefore(InstallmentStatus.OPEN, LocalDate.now());
        due.forEach(i -> {
            i.setStatus(InstallmentStatus.OVERDUE);
            notificationService.notifyOverdue(i);
        });
        repository.saveAll(due);
        return due.size();
    }

    /**
     * Envia lembretes para parcelas em aberto que vencem nos próximos N dias
     * (N configurável nas Configurações). Disparado pelo job agendado.
     */
    @Transactional(readOnly = true)
    public int remindDueSoon() {
        int daysBefore = notificationService.reminderDays();
        LocalDate target = LocalDate.now().plusDays(daysBefore);
        List<Installment> due = repository.findByStatusAndDueDate(InstallmentStatus.OPEN, target);
        due.forEach(i -> notificationService.notifyDueSoon(i, daysBefore));
        return due.size();
    }

    public Installment getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Parcela", id));
    }
}
