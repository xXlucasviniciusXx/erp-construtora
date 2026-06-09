package com.construtora.financeiro.service.distrato;

import com.construtora.financeiro.dto.distrato.*;
import com.construtora.financeiro.dto.payable.PayableRequest;
import com.construtora.financeiro.dto.receivable.ReceivableRequest;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.DistratoMapper;
import com.construtora.financeiro.model.*;
import com.construtora.financeiro.model.enums.*;
import com.construtora.financeiro.repository.DistratoRepository;
import com.construtora.financeiro.repository.InstallmentRepository;
import com.construtora.financeiro.repository.LotRepository;
import com.construtora.financeiro.repository.PropertySaleRepository;
import com.construtora.financeiro.security.SecurityUtils;
import com.construtora.financeiro.service.AuditService;
import com.construtora.financeiro.service.LateFeeCalculator;
import com.construtora.financeiro.service.LateFeeCalculator.LateFees;
import com.construtora.financeiro.service.PayableService;
import com.construtora.financeiro.service.ReceivableService;
import com.construtora.financeiro.service.distrato.DistratoCalculator.DistratoCalculation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DistratoService {

    private static final List<DistratoStatus> OPEN_STATUSES = List.of(
            DistratoStatus.SOLICITADO, DistratoStatus.APROVADO, DistratoStatus.AGUARDANDO_QUITACAO_FINANCEIRA);

    private final DistratoRepository repository;
    private final DistratoConfigService configService;
    private final PropertySaleRepository saleRepository;
    private final LotRepository lotRepository;
    private final InstallmentRepository installmentRepository;
    private final LateFeeCalculator lateFeeCalculator;
    private final PayableService payableService;
    private final ReceivableService receivableService;
    private final AuditService auditService;
    private final DistratoMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DistratoService(DistratoRepository repository, DistratoConfigService configService,
                           PropertySaleRepository saleRepository, LotRepository lotRepository,
                           InstallmentRepository installmentRepository, LateFeeCalculator lateFeeCalculator,
                           PayableService payableService, ReceivableService receivableService,
                           AuditService auditService, DistratoMapper mapper) {
        this.repository = repository;
        this.configService = configService;
        this.saleRepository = saleRepository;
        this.lotRepository = lotRepository;
        this.installmentRepository = installmentRepository;
        this.lateFeeCalculator = lateFeeCalculator;
        this.payableService = payableService;
        this.receivableService = receivableService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    // ===================== Consultas =====================

    @Transactional(readOnly = true)
    public List<DistratoResponse> list(DistratoStatus status) {
        var entities = status != null
                ? repository.findByStatusOrderByCreatedAtDesc(status)
                : repository.findAllByOrderByCreatedAtDesc();
        return entities.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DistratoResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    // ===================== Simulação =====================

    @Transactional(readOnly = true)
    public DistratoSimulationResponse simulate(DistratoSimulationRequest req) {
        PropertySale sale = getSale(req.saleId());
        return buildSimulation(sale, req.usedRetentionPercent(), req.financialRule());
    }

    private DistratoSimulationResponse buildSimulation(PropertySale sale,
                                                       BigDecimal overridePercent,
                                                       DistratoFinancialRule overrideRule) {
        Lot lot = sale.getLot();
        Block block = lot.getBlock();
        Development dev = block.getDevelopment();

        SaleFinancials fin = computeFinancials(sale);
        BigDecimal defaultPercent = lot.getRetentionPercent();
        BigDecimal usedPercent = overridePercent != null ? overridePercent
                : (defaultPercent != null ? defaultPercent : BigDecimal.ZERO);
        DistratoFinancialRule rule = overrideRule != null ? overrideRule
                : configService.resolveRule(dev.getId());

        DistratoCalculation calc = DistratoCalculator.calculate(
                rule, fin.paid(), usedPercent, fin.overdue(), fin.charges(), fin.totalDebt());

        return new DistratoSimulationResponse(
                sale.getId(), sale.getContractNumber(), sale.getClient().getName(),
                dev.getName(), block.getName(), lot.getName(),
                sale.getTotalValue(), calc.paidAmount(), defaultPercent, usedPercent,
                calc.retentionAmount(), rule,
                calc.overdueAmount(), calc.chargesAmount(), calc.totalDebtAmount(),
                calc.deductions(), calc.finalBalance(), calc.outcome(), calc.financialEntryAmount(),
                sale.getDistratoReason());
    }

    // ===================== Solicitação =====================

    public DistratoResponse request(DistratoCreateRequest req) {
        PropertySale sale = getSale(req.saleId());
        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new BusinessException("Somente vendas ATIVAS podem ser distratadas. Status atual: " + sale.getStatus());
        }
        if (repository.existsBySaleIdAndStatusIn(sale.getId(), OPEN_STATUSES)) {
            throw new BusinessException("Já existe um distrato em andamento para esta venda.");
        }

        Lot lot = sale.getLot();
        Block block = lot.getBlock();
        Development dev = block.getDevelopment();

        SaleFinancials fin = computeFinancials(sale);
        BigDecimal defaultPercent = lot.getRetentionPercent();
        BigDecimal usedPercent = req.usedRetentionPercent() != null ? req.usedRetentionPercent()
                : (defaultPercent != null ? defaultPercent : BigDecimal.ZERO);
        DistratoFinancialRule rule = req.financialRule() != null ? req.financialRule()
                : configService.resolveRule(dev.getId());

        boolean changedPercent = defaultPercent == null
                ? usedPercent.signum() != 0
                : usedPercent.compareTo(defaultPercent) != 0;
        if (changedPercent && (req.retentionChangeReason() == null || req.retentionChangeReason().isBlank())) {
            throw new BusinessException("Informe o motivo da alteração do percentual de retenção (diferente do padrão do lote).");
        }

        DistratoCalculation calc = DistratoCalculator.calculate(
                rule, fin.paid(), usedPercent, fin.overdue(), fin.charges(), fin.totalDebt());

        Distrato d = new Distrato();
        d.setSale(sale);
        d.setClient(sale.getClient());
        d.setLot(lot);
        d.setDevelopmentName(dev.getName());
        d.setBlockName(block.getName());
        d.setLotName(lot.getName());
        d.setStatus(DistratoStatus.SOLICITADO);
        d.setFinancialRule(rule);
        d.setReason(req.reason());
        d.setContractTotal(sale.getTotalValue());
        d.setPaidAmount(calc.paidAmount());
        d.setDefaultRetentionPercent(defaultPercent);
        d.setUsedRetentionPercent(usedPercent);
        d.setRetentionChangeReason(changedPercent ? req.retentionChangeReason() : null);
        d.setRetentionAmount(calc.retentionAmount());
        d.setOverdueAmount(calc.overdueAmount());
        d.setChargesAmount(calc.chargesAmount());
        d.setTotalDebtAmount(calc.totalDebtAmount());
        d.setFinalBalance(calc.finalBalance());
        d.setFinancialOutcome(calc.outcome());
        d.setRequestedBy(currentUser());
        d.setRequestedAt(now());
        d.setCalculationMemory(buildMemoryJson(sale, calc, rule, defaultPercent, usedPercent,
                req.retentionChangeReason(), changedPercent));

        // Bloqueia o lote: não pode ser vendido/reservado enquanto em distrato.
        lot.setStatus(PropertyStatus.EM_DISTRATO);
        lotRepository.save(lot);

        Distrato saved = repository.save(d);
        auditService.log("DISTRATO_REQUEST", "distratos", saved.getId(),
                "Distrato solicitado · contrato " + sale.getContractNumber()
                        + " · regra " + rule + " · retenção padrão "
                        + (defaultPercent != null ? defaultPercent : "—") + "% · usada " + usedPercent + "%"
                        + (changedPercent ? " · motivo alteração: " + req.retentionChangeReason() : ""));
        return mapper.toResponse(saved);
    }

    // ===================== Aprovação =====================

    public DistratoResponse approve(UUID id) {
        Distrato d = getEntity(id);
        if (d.getStatus() != DistratoStatus.SOLICITADO) {
            throw new BusinessException("Apenas distratos SOLICITADOS podem ser aprovados. Status atual: " + d.getStatus());
        }
        d.setStatus(DistratoStatus.APROVADO);
        d.setApprovedBy(currentUser());
        d.setApprovedAt(now());

        DistratoFinancialOutcome outcome = d.getFinancialOutcome();
        BigDecimal entry = d.getFinalBalance() != null ? d.getFinalBalance().abs() : BigDecimal.ZERO;

        if (outcome == DistratoFinancialOutcome.ZERO) {
            // Saldo zero: conclui imediatamente, sem lançamento financeiro.
            auditService.log("DISTRATO_APPROVE", "distratos", d.getId(),
                    "Aprovado com saldo zero · conclusão imediata · contrato " + d.getSale().getContractNumber());
            conclude(d, currentUser());
            return mapper.toResponse(d);
        }

        UUID devId = d.getLot().getBlock().getDevelopment().getId();
        if (outcome == DistratoFinancialOutcome.PAYABLE) {
            var payable = payableService.create(new PayableRequest(
                    "Distrato - " + d.getClient().getName(), null,
                    "Distrato contrato " + d.getSale().getContractNumber() + " — devolução ao cliente",
                    entry, LocalDate.now(), null, PayableStatus.OPEN, null, null, devId, null));
            d.setPayableId(payable.id());
        } else {
            var receivable = receivableService.create(new ReceivableRequest(
                    d.getClient().getId(), d.getSale().getId(), null, null,
                    "Distrato contrato " + d.getSale().getContractNumber() + " — saldo devido pelo cliente",
                    entry, LocalDate.now(), null, ReceivableStatus.OPEN, null,
                    "Lançamento gerado automaticamente pelo distrato " + d.getId()));
            d.setReceivableId(receivable.id());
        }
        d.setStatus(DistratoStatus.AGUARDANDO_QUITACAO_FINANCEIRA);
        Distrato saved = repository.save(d);
        auditService.log("DISTRATO_APPROVE", "distratos", saved.getId(),
                "Aprovado · " + outcome + " R$ " + entry + " · contrato " + d.getSale().getContractNumber());
        return mapper.toResponse(saved);
    }

    // ===================== Quitação financeira =====================

    public DistratoResponse settle(UUID id, DistratoSettleRequest req) {
        Distrato d = getEntity(id);
        if (d.getStatus() != DistratoStatus.AGUARDANDO_QUITACAO_FINANCEIRA) {
            throw new BusinessException("Apenas distratos aguardando quitação podem ser baixados. Status atual: " + d.getStatus());
        }
        LocalDate date = req != null && req.settleDate() != null ? req.settleDate() : LocalDate.now();
        if (d.getFinancialOutcome() == DistratoFinancialOutcome.PAYABLE && d.getPayableId() != null) {
            payableService.confirmPayment(d.getPayableId(), date);
        } else if (d.getFinancialOutcome() == DistratoFinancialOutcome.RECEIVABLE && d.getReceivableId() != null) {
            receivableService.confirmReceive(d.getReceivableId(), date);
        }
        d.setSettledBy(currentUser());
        auditService.log("DISTRATO_SETTLE", "distratos", d.getId(),
                "Quitação financeira registrada · contrato " + d.getSale().getContractNumber());
        conclude(d, currentUser());
        return mapper.toResponse(d);
    }

    // ===================== Cancelamento =====================

    public DistratoResponse cancel(UUID id) {
        Distrato d = getEntity(id);
        if (d.getStatus() == DistratoStatus.CONCLUIDO || d.getStatus() == DistratoStatus.CANCELADO) {
            throw new BusinessException("Distrato já finalizado não pode ser cancelado. Status: " + d.getStatus());
        }
        // Cancela lançamentos financeiros pendentes, se houver.
        if (d.getPayableId() != null) {
            try { payableService.cancel(d.getPayableId()); } catch (RuntimeException ignored) { }
        }
        if (d.getReceivableId() != null) {
            try { receivableService.cancel(d.getReceivableId()); } catch (RuntimeException ignored) { }
        }
        // Devolve o lote ao estado vendido (a venda permanece ativa).
        Lot lot = d.getLot();
        if (lot.getStatus() == PropertyStatus.EM_DISTRATO) {
            lot.setStatus(PropertyStatus.SOLD);
            lotRepository.save(lot);
        }
        d.setStatus(DistratoStatus.CANCELADO);
        Distrato saved = repository.save(d);
        auditService.log("DISTRATO_CANCEL", "distratos", saved.getId(),
                "Distrato cancelado · contrato " + d.getSale().getContractNumber());
        return mapper.toResponse(saved);
    }

    // ===================== Conclusão =====================

    private void conclude(Distrato d, UUID settledBy) {
        if (d.getStatus() == DistratoStatus.CONCLUIDO) return;

        PropertySale sale = d.getSale();
        sale.setStatus(SaleStatus.DISTRATADO);
        sale.setDistratoDate(LocalDate.now());
        sale.setDistratoReason(d.getReason());
        sale.setDistratoRetainedAmount(d.getRetentionAmount());
        sale.setDistratoRefundAmount(
                d.getFinancialOutcome() == DistratoFinancialOutcome.PAYABLE
                        ? d.getFinalBalance().abs() : BigDecimal.ZERO);
        sale.setDistratoRule(d.getFinancialRule().name());
        sale.setDistratoRuleDetail(d.getCalculationMemory());

        // Cancela parcelas futuras não pagas; mantém as pagas no histórico.
        for (Installment i : sale.getInstallments()) {
            if (i.getStatus() != InstallmentStatus.PAID && i.getStatus() != InstallmentStatus.CANCELLED) {
                i.setStatus(InstallmentStatus.CANCELLED);
            }
        }
        installmentRepository.saveAll(sale.getInstallments());
        saleRepository.save(sale);

        // Libera o lote para nova venda.
        Lot lot = d.getLot();
        lot.setStatus(PropertyStatus.AVAILABLE);
        lot.setSaleValue(null);
        lotRepository.save(lot);

        d.setStatus(DistratoStatus.CONCLUIDO);
        d.setConcludedAt(now());
        if (d.getSettledBy() == null) d.setSettledBy(settledBy);
        repository.save(d);
        auditService.log("DISTRATO_CONCLUDE", "distratos", d.getId(),
                "Distrato concluído · contrato " + sale.getContractNumber() + " marcado DISTRATADO · lote liberado");
    }

    // ===================== Helpers =====================

    /** Valor pago, parcelas vencidas em aberto, encargos e saldo devedor total. */
    private SaleFinancials computeFinancials(PropertySale sale) {
        LocalDate today = LocalDate.now();
        BigDecimal paid = orZero(sale.getDownPayment());   // entrada conta como valor pago
        BigDecimal overdue = BigDecimal.ZERO;
        BigDecimal charges = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;

        for (Installment i : sale.getInstallments()) {
            if (i.getStatus() == InstallmentStatus.PAID) {
                paid = paid.add(orZero(i.getAmount()));
            } else if (i.getStatus() != InstallmentStatus.CANCELLED) {
                totalDebt = totalDebt.add(orZero(i.getAmount()));
                boolean isOverdue = i.getStatus() == InstallmentStatus.OVERDUE
                        || (i.getDueDate() != null && i.getDueDate().isBefore(today));
                if (isOverdue) {
                    overdue = overdue.add(orZero(i.getAmount()));
                    LateFees f = lateFeeCalculator.compute(i, today);
                    charges = charges.add(orZero(f.penaltyAmount())).add(orZero(f.interestAmount()));
                }
            }
        }
        return new SaleFinancials(paid, overdue, charges, totalDebt);
    }

    private String buildMemoryJson(PropertySale sale, DistratoCalculation calc, DistratoFinancialRule rule,
                                   BigDecimal defaultPercent, BigDecimal usedPercent,
                                   String retentionChangeReason, boolean changedPercent) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNumber", sale.getContractNumber());
        m.put("contractTotal", sale.getTotalValue());
        m.put("paidAmount", calc.paidAmount());
        m.put("financialRule", rule.name());
        m.put("defaultRetentionPercent", defaultPercent);
        m.put("usedRetentionPercent", usedPercent);
        m.put("retentionChanged", changedPercent);
        m.put("retentionChangeReason", retentionChangeReason);
        m.put("retentionAmount", calc.retentionAmount());
        m.put("overdueAmount", calc.overdueAmount());
        m.put("chargesAmount", calc.chargesAmount());
        m.put("totalDebtAmount", calc.totalDebtAmount());
        m.put("deductions", calc.deductions());
        m.put("finalBalance", calc.finalBalance());
        m.put("financialOutcome", calc.outcome().name());
        m.put("financialEntryAmount", calc.financialEntryAmount());
        m.put("formula", "finalBalance = paidAmount - retentionAmount - deductions");
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private PropertySale getSale(UUID id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Venda", id));
    }

    public Distrato getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Distrato", id));
    }

    private static UUID currentUser() {
        return SecurityUtils.currentUserId().orElse(null);
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private record SaleFinancials(BigDecimal paid, BigDecimal overdue, BigDecimal charges, BigDecimal totalDebt) {}
}
