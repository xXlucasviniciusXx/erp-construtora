package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.PropertyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Lote — vinculado a uma Quadra. Substitui a antiga entidade plana Property. */
@Entity
@Table(name = "lots")
@Getter
@Setter
public class Lot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "block_id")
    private Block block;

    @Column(nullable = false)
    private String name;

    /** Código interno gerado pelo sistema (ex.: E001-Q01-L001). */
    @Column(name = "internal_code", nullable = false, unique = true)
    private String internalCode;

    /** Matrícula do cartório (manual; pode ficar vazia). */
    private String registration;

    private String unit;
    private String address;

    @Column(name = "total_area")
    private BigDecimal totalArea;

    @Column(name = "built_area")
    private BigDecimal builtArea;

    /** Valor previsto de venda do lote. */
    @Column(name = "planned_value")
    private BigDecimal plannedValue;

    /** Valor pelo qual o lote foi realmente vendido (alimentado pela venda). */
    @Column(name = "sale_value")
    private BigDecimal saleValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @Column(name = "contract_extra", columnDefinition = "text")
    private String contractExtra;

    @Column(columnDefinition = "text")
    private String notes;

    /** Expiração da reserva. Nulo se o lote não estiver RESERVED. */
    @Column(name = "reservation_expires_at")
    private java.time.LocalDateTime reservationExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
