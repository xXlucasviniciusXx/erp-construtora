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

@Entity
@Table(name = "properties")
@Getter
@Setter
public class Property {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String development;

    private String block;
    private String lot;
    private String unit;
    private String registration;
    private String address;

    @Column(name = "total_area")
    private BigDecimal totalArea;

    @Column(name = "built_area")
    private BigDecimal builtArea;

    @Column(name = "sale_value")
    private BigDecimal saleValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @Column(name = "contract_extra", columnDefinition = "text")
    private String contractExtra;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
