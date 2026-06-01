package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "property_sales")
@Getter
@Setter
public class PropertySale {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue;

    @Column(name = "down_payment", nullable = false)
    private BigDecimal downPayment = BigDecimal.ZERO;

    @Column(name = "installments_count", nullable = false)
    private Integer installmentsCount;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    /** Forma de compra: À vista / Entrada + parcelas / Financiamento próprio. */
    @Column(name = "purchase_type")
    private String purchaseType;

    @Column(name = "correction_index")
    private String correctionIndex;

    @Column(name = "interest_rate")
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "penalty_rate")
    private BigDecimal penaltyRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status = SaleStatus.ACTIVE;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate = LocalDate.now();

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<Installment> installments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
