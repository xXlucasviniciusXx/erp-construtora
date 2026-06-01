package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Empreendimento — nível mais alto da hierarquia (Empreendimento → Quadra → Lote). */
@Entity
@Table(name = "developments")
@Getter
@Setter
public class Development {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Código interno gerado pelo sistema (ex.: E001). */
    @Column(name = "internal_code", nullable = false, unique = true)
    private String internalCode;

    /** Quantidade prevista de quadras (limite). */
    @Column(name = "blocks_count")
    private Integer blocksCount;

    /** Quantidade prevista de lotes no empreendimento todo (limite). */
    @Column(name = "lots_count")
    private Integer lotsCount;

    /** Valor expectativa do empreendimento (manual/editável). */
    @Column(name = "expected_value", nullable = false)
    private BigDecimal expectedValue = BigDecimal.ZERO;

    private String address;

    @Column(nullable = false)
    private String status = "ACTIVE";

    private String dimensions;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
