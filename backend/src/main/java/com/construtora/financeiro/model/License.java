package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Licença única desta instalação (sem multitenant ainda). Guarda plano,
 * período de validade e status. Hoje é só informativo (avisos de
 * vencimento) — não bloqueia nenhuma funcionalidade.
 */
@Entity
@Table(name = "license")
@Getter
@Setter
public class License {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String plan = "PROFISSIONAL";

    /** Status definido manualmente: ATIVA, SUSPENSA, CANCELADA. */
    @Column(nullable = false)
    private String status = "ATIVA";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate = LocalDate.now();

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "period_months", nullable = false)
    private int periodMonths = 12;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(length = 500)
    private String notes;

    /** Nome do cliente dono da licença (informativo / vem na chave). */
    private String customer;

    /** Chave de licenciamento (token HMAC) que originou esta licença, se houver. */
    @Column(name = "license_key", columnDefinition = "text")
    private String licenseKey;

    /** Dias de tolerância após o vencimento antes de entrar em modo só-leitura. */
    @Column(name = "grace_days", nullable = false)
    private int graceDays = 7;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
