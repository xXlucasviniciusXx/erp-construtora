package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.FileFormat;
import com.construtora.financeiro.model.enums.ImportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_statement_imports")
@Getter
@Setter
public class BankStatementImport {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false)
    private FileFormat fileFormat;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status = ImportStatus.PROCESSING;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "imported_rows")
    private Integer importedRows = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "imported_by")
    private UUID importedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
