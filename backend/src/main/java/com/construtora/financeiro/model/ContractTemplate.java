package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo de documento (contrato/distrato) editável pelo ADMIN. O corpo é XHTML
 * com tokens {@code {{...}}} substituídos na geração (ver ContractRenderer).
 */
@Entity
@Table(name = "contract_templates")
@Getter
@Setter
public class ContractTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    /** CONTRACT ou DISTRATO. */
    @Column(nullable = false)
    private String kind = "CONTRACT";

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
