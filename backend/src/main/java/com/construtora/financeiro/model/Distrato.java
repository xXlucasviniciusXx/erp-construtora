package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.DistratoFinancialOutcome;
import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import com.construtora.financeiro.model.enums.DistratoStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Distrato (rescisão amigável) de um contrato/venda de lote. Mantém todo o
 * histórico financeiro e de fluxo (solicitação → aprovação → quitação →
 * conclusão), incluindo a memória de cálculo completa em JSON.
 */
@Entity
@Table(name = "distratos")
@Getter
@Setter
public class Distrato {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id")
    private PropertySale sale;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(name = "development_name")
    private String developmentName;
    @Column(name = "block_name")
    private String blockName;
    @Column(name = "lot_name")
    private String lotName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DistratoStatus status = DistratoStatus.SOLICITADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_rule", nullable = false, length = 60)
    private DistratoFinancialRule financialRule;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "contract_total")
    private BigDecimal contractTotal = BigDecimal.ZERO;
    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "default_retention_percent")
    private BigDecimal defaultRetentionPercent;
    @Column(name = "used_retention_percent")
    private BigDecimal usedRetentionPercent = BigDecimal.ZERO;
    @Column(name = "retention_change_reason", columnDefinition = "text")
    private String retentionChangeReason;
    @Column(name = "retention_amount")
    private BigDecimal retentionAmount = BigDecimal.ZERO;
    @Column(name = "overdue_amount")
    private BigDecimal overdueAmount = BigDecimal.ZERO;
    @Column(name = "charges_amount")
    private BigDecimal chargesAmount = BigDecimal.ZERO;
    @Column(name = "total_debt_amount")
    private BigDecimal totalDebtAmount = BigDecimal.ZERO;
    @Column(name = "final_balance")
    private BigDecimal finalBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_outcome", nullable = false, length = 20)
    private DistratoFinancialOutcome financialOutcome = DistratoFinancialOutcome.ZERO;

    @Column(name = "payable_id")
    private UUID payableId;
    @Column(name = "receivable_id")
    private UUID receivableId;

    @Column(name = "requested_by")
    private UUID requestedBy;
    @Column(name = "approved_by")
    private UUID approvedBy;
    @Column(name = "settled_by")
    private UUID settledBy;
    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "concluded_at")
    private OffsetDateTime concludedAt;

    /** Memória de cálculo completa em JSON. */
    @Column(name = "calculation_memory", columnDefinition = "text")
    private String calculationMemory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
