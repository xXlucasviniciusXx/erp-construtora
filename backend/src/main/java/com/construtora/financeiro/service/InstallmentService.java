package com.construtora.financeiro.service;

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
    private final SaleMapper mapper;

    public InstallmentService(InstallmentRepository repository,
                              NotificationService notificationService, SaleMapper mapper) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> findBySale(UUID saleId) {
        return repository.findBySaleId(saleId).stream().map(mapper::toInstallmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> findOverdue() {
        return repository.findOverdueUnpaid(LocalDate.now()).stream()
                .map(mapper::toInstallmentResponse).toList();
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

    public Installment getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Parcela", id));
    }
}
