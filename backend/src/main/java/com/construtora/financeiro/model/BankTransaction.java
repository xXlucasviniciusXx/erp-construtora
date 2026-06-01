package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.TransactionStatus;
import com.construtora.financeiro.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
public class BankTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "import_id")
    private BankStatementImport statementImport;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    private String description;

    /** Positivo = crédito, negativo = débito. */
    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "bank_identifier")
    private String bankIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    /** Motivo da divergência ou anotação da conciliação manual. */
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
