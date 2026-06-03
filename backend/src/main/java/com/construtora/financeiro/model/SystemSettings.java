package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "system_name", nullable = false)
    private String systemName = "Construtora Financeiro";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", nullable = false)
    private String primaryColor = "#1e40af";

    @Column(name = "secondary_color", nullable = false)
    private String secondaryColor = "#0f766e";

    @Column(nullable = false)
    private String theme = "light";

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_document")
    private String companyDocument;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "company_phone")
    private String companyPhone;

    @Column(name = "company_email")
    private String companyEmail;

    @Column(name = "footer_text")
    private String footerText;

    // ---- Configuração de e-mail (SMTP), editável pela tela de Configurações ----
    @Column(name = "mail_enabled", nullable = false)
    private boolean mailEnabled = false;

    @Column(name = "mail_host")
    private String mailHost;

    @Column(name = "mail_port")
    private Integer mailPort = 587;

    @Column(name = "mail_username")
    private String mailUsername;

    @Column(name = "mail_password")
    private String mailPassword;

    @Column(name = "mail_from")
    private String mailFrom;

    /** Dias de antecedência para o lembrete de vencimento. */
    @Column(name = "mail_reminder_days", nullable = false)
    private Integer mailReminderDays = 3;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
