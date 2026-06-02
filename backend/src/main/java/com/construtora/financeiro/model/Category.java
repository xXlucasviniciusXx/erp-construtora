package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Categoria financeira (natureza da despesa) — plano de contas em 2 níveis:
 * {@code grupo} → {@code name}. Ex.: grupo "Infraestrutura e Obras", item "Terraplanagem".
 */
@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = {"grupo", "name"}))
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String grupo;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
