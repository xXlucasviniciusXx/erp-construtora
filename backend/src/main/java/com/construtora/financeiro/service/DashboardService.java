package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.dashboard.DashboardResponse;
import com.construtora.financeiro.model.enums.*;
import com.construtora.financeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final InstallmentRepository installmentRepository;
    private final AccountReceivableRepository receivableRepository;
    private final AccountPayableRepository payableRepository;
    private final BankTransactionRepository transactionRepository;
    private final PropertyRepository propertyRepository;

    public DashboardService(InstallmentRepository installmentRepository,
                            AccountReceivableRepository receivableRepository,
                            AccountPayableRepository payableRepository,
                            BankTransactionRepository transactionRepository,
                            PropertyRepository propertyRepository) {
        this.installmentRepository = installmentRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.transactionRepository = transactionRepository;
        this.propertyRepository = propertyRepository;
    }

    public DashboardResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal receivableFromInstallments =
                installmentRepository.sumByStatusAndDueDateBetween(InstallmentStatus.OPEN, monthStart, monthEnd);
        BigDecimal receivableFromAccounts =
                receivableRepository.sumByStatusAndDueDateBetween(ReceivableStatus.OPEN, monthStart, monthEnd);
        BigDecimal totalReceivable = receivableFromInstallments.add(receivableFromAccounts);

        BigDecimal totalPayable =
                payableRepository.sumByStatusAndDueDateBetween(PayableStatus.OPEN, monthStart, monthEnd);

        long overdue = installmentRepository.countByStatus(InstallmentStatus.OVERDUE);
        long upcoming = installmentRepository.findByStatusAndDueDateBefore(InstallmentStatus.OPEN, monthEnd.plusDays(1))
                .stream().filter(i -> !i.getDueDate().isBefore(today)).count();
        long pendingTxn = transactionRepository.countByStatus(TransactionStatus.PENDING);
        long available = propertyRepository.countByStatus(PropertyStatus.AVAILABLE);
        long sold = propertyRepository.countByStatus(PropertyStatus.SOLD);

        return new DashboardResponse(
                totalReceivable, totalPayable, totalReceivable.subtract(totalPayable),
                overdue, upcoming, pendingTxn, available, sold);
    }
}
