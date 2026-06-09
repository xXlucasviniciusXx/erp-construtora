package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Regra financeira de distrato. {@code development == null} representa a
 * configuração GLOBAL; quando um empreendimento possui registro próprio, ele
 * prevalece sobre a global.
 */
@Entity
@Table(name = "distrato_configs")
@Getter
@Setter
public class DistratoConfig {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "development_id")
    private Development development;   // null = global

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_rule", nullable = false, length = 60)
    private DistratoFinancialRule financialRule = DistratoFinancialRule.RETENCAO_MAIS_PARCELAS_VENCIDAS;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
