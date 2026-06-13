package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.InstallmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "installments")
@Getter
@Setter
public class Installment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id")
    private PropertySale sale;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Data do crédito no extrato bancário, capturada na conciliação (uso futuro).
     * É um registro informativo; a referência financeira principal continua sendo
     * a data da baixa ({@link #paymentDate}).
     */
    @Column(name = "bank_credit_date")
    private LocalDate bankCreditDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status = InstallmentStatus.OPEN;

    /**
     * Composição do valor recebido na baixa: principal (valor da parcela), juros
     * de mora e multa por atraso. Preenchido na baixa (manual ou por conciliação);
     * nulo enquanto não pago. Distingue receita de vendas de receita financeira.
     */
    @Column(name = "paid_principal")
    private BigDecimal paidPrincipal;
    @Column(name = "paid_interest")
    private BigDecimal paidInterest;
    @Column(name = "paid_penalty")
    private BigDecimal paidPenalty;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
