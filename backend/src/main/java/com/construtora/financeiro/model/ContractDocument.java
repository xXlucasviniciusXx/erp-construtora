package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cópia arquivada de um documento (contrato/distrato) gerado para uma venda.
 * Mantém o PDF no banco e versiona cada nova geração para o mesmo tipo.
 */
@Entity
@Table(name = "contract_documents")
@Getter
@Setter
public class ContractDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private PropertySale sale;

    /** CONTRACT ou DISTRATO. */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "pdf_data", nullable = false)
    private byte[] pdfData;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "generated_by")
    private String generatedBy;
}
