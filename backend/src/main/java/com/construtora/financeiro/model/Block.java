package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Quadra — vinculada a um Empreendimento. */
@Entity
@Table(name = "blocks")
@Getter
@Setter
public class Block {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "development_id")
    private Development development;

    @Column(nullable = false)
    private String name;

    /** Código interno gerado pelo sistema (ex.: E001-Q01). */
    @Column(name = "internal_code", nullable = false, unique = true)
    private String internalCode;

    /** Matrícula do cartório (manual; pode ficar vazia até o registro sair). */
    private String registration;

    private BigDecimal area;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
