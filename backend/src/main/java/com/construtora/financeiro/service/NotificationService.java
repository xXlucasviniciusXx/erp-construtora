package com.construtora.financeiro.service;

import com.construtora.financeiro.model.EmailNotification;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.SystemSettings;
import com.construtora.financeiro.repository.EmailNotificationRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.UUID;

/**
 * Centraliza o envio de e-mails transacionais (HTML, com a identidade do sistema).
 * A configuração de SMTP é lida do banco ({@code system_settings}), editável pela
 * tela de Configurações. Toda notificação é persistida em {@code email_notifications};
 * o envio só ocorre quando o e-mail está habilitado e há um host configurado —
 * caso contrário fica registrada como PENDING e é logada (modo simulado).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailNotificationRepository repository;
    private final SettingsService settingsService;

    public NotificationService(EmailNotificationRepository repository, SettingsService settingsService) {
        this.repository = repository;
        this.settingsService = settingsService;
    }

    // ---------------- Eventos ----------------

    public void notifyOverdue(Installment i) {
        String to = recipientOf(i);
        if (to == null) return;
        String inner = "<p>Olá <strong>" + esc(clientName(i)) + "</strong>,</p>"
                + "<p>Identificamos que a parcela <strong>#" + i.getNumber() + "</strong> no valor de <strong>"
                + brl(i.getAmount()) + "</strong>, com vencimento em <strong>" + i.getDueDate()
                + "</strong>, encontra-se <strong>em atraso</strong>.</p>"
                + "<p>Por favor, regularize o pagamento o quanto antes para evitar encargos adicionais.</p>";
        send(to, "Parcela em atraso #" + i.getNumber(), inner, "PAYMENT_OVERDUE");
    }

    public void notifyDueSoon(Installment i, int daysBefore) {
        String to = recipientOf(i);
        if (to == null) return;
        String inner = "<p>Olá <strong>" + esc(clientName(i)) + "</strong>,</p>"
                + "<p>Este é um lembrete de que a parcela <strong>#" + i.getNumber() + "</strong> no valor de <strong>"
                + brl(i.getAmount()) + "</strong> vence em <strong>" + daysBefore + " dia(s)</strong> ("
                + i.getDueDate() + ").</p>"
                + "<p>Se já efetuou o pagamento, desconsidere este aviso.</p>";
        send(to, "Lembrete de vencimento — parcela #" + i.getNumber(), inner, "PAYMENT_DUE_SOON");
    }

    public void notifyPaymentConfirmed(Installment i) {
        String to = recipientOf(i);
        if (to == null) return;
        String inner = "<p>Olá <strong>" + esc(clientName(i)) + "</strong>,</p>"
                + "<p>Recebemos o pagamento da parcela <strong>#" + i.getNumber() + "</strong> no valor de <strong>"
                + brl(i.getAmount()) + "</strong>. Obrigado!</p>";
        send(to, "Pagamento confirmado — parcela #" + i.getNumber(), inner, "PAYMENT_CONFIRMED");
    }

    public void notifySaleCreated(PropertySale sale) {
        String to = sale.getClient().getEmail();
        if (to == null || to.isBlank()) return;
        String inner = "<p>Olá <strong>" + esc(sale.getClient().getName()) + "</strong>,</p>"
                + "<p>Sua compra foi registrada no valor de <strong>" + brl(sale.getTotalValue())
                + "</strong> em <strong>" + sale.getInstallmentsCount() + " parcelas</strong>.</p>";
        send(to, "Venda registrada", inner, "SALE_CREATED");
    }

    public void notifyContractGenerated(PropertySale sale) {
        String to = sale.getClient().getEmail();
        if (to == null || to.isBlank()) return;
        send(to, "Contrato gerado", "<p>O contrato referente à sua compra foi gerado e está disponível.</p>",
                "CONTRACT_GENERATED");
    }

    /** Envia um e-mail de teste para validar a configuração de SMTP. */
    public EmailNotification sendTest(String to) {
        String inner = "<p>Este é um <strong>e-mail de teste</strong> do sistema.</p>"
                + "<p>Se você recebeu esta mensagem, a configuração de envio (SMTP) está funcionando.</p>";
        return send(to, "E-mail de teste", inner, "TEST");
    }

    // ---------------- Listagem / reenvio ----------------

    public org.springframework.data.domain.Page<EmailNotification> list(
            String status, String eventType, org.springframework.data.domain.Pageable pageable) {
        return repository.search(blankToEmpty(status), blankToEmpty(eventType), pageable);
    }

    /** Reenvia uma notificação já registrada (usa o corpo HTML armazenado). */
    public EmailNotification resend(UUID id) {
        EmailNotification record = repository.findById(id)
                .orElseThrow(() -> com.construtora.financeiro.exception.ResourceNotFoundException.of("Notificação", id));
        dispatch(record);
        return repository.save(record);
    }

    /** Dias de antecedência configurados para o lembrete de vencimento. */
    public int reminderDays() {
        Integer d = settingsService.current().getMailReminderDays();
        return d != null && d > 0 ? d : 3;
    }

    // ---------------- Internos ----------------

    private EmailNotification send(String to, String subject, String innerHtml, String eventType) {
        EmailNotification record = new EmailNotification();
        record.setRecipient(to);
        record.setSubject(subject);
        record.setBody(template(subject, innerHtml));
        record.setEventType(eventType);
        dispatch(record);
        return repository.save(record);
    }

    /** Faz a entrega (SMTP real ou simulada) e atualiza o status do registro. */
    private void dispatch(EmailNotification record) {
        SystemSettings s = settingsService.current();
        if (!s.isMailEnabled() || s.getMailHost() == null || s.getMailHost().isBlank()) {
            log.info("[E-MAIL SIMULADO] para={} assunto='{}' evento={}",
                    record.getRecipient(), record.getSubject(), record.getEventType());
            record.setStatus("PENDING");
            record.setError(null);
            return;
        }
        try {
            JavaMailSender sender = buildSender(s);
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            InternetAddress from = senderAddress(s);
            helper.setFrom(from);
            helper.setTo(record.getRecipient());
            helper.setSubject(record.getSubject());
            helper.setText(record.getBody(), true); // HTML
            sender.send(mime);
            record.setStatus("SENT");
            record.setSentAt(OffsetDateTime.now());
            record.setError(null);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}", record.getRecipient(), e);
            record.setStatus("FAILED");
            record.setError(e.getMessage());
        }
    }

    private JavaMailSender buildSender(SystemSettings s) {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(s.getMailHost());
        impl.setPort(s.getMailPort() != null ? s.getMailPort() : 587);
        if (s.getMailUsername() != null) impl.setUsername(s.getMailUsername());
        if (s.getMailPassword() != null) impl.setPassword(s.getMailPassword());
        impl.setDefaultEncoding("UTF-8");

        Properties props = impl.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", (s.getMailUsername() != null && !s.getMailUsername().isBlank()) ? "true" : "false");

        // STARTTLS (porta 587 — padrão MailerSend/SendGrid/etc.)
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.starttls.required",  "true");   // obriga STARTTLS; falha se servidor não suportar
        props.put("mail.smtp.ssl.trust",          s.getMailHost() != null ? s.getMailHost() : "*");
        props.put("mail.smtp.ssl.protocols",      "TLSv1.2 TLSv1.3");

        // Timeouts (25 s — free tier pode ser lento)
        props.put("mail.smtp.connectiontimeout", "25000");
        props.put("mail.smtp.timeout",           "25000");
        props.put("mail.smtp.writetimeout",      "25000");

        log.debug("[SMTP] host={} port={} user={} from={}",
                s.getMailHost(), s.getMailPort(),
                s.getMailUsername(), mailFrom(s));
        return impl;
    }

    private String mailFrom(SystemSettings s) {
        if (s.getMailFrom() != null && !s.getMailFrom().isBlank()) return s.getMailFrom();
        if (s.getMailUsername() != null && !s.getMailUsername().isBlank()) return s.getMailUsername();
        return "nao-responder@construtora.com.br";
    }

    /**
     * Monta o endereço de remetente com display name amigável.
     * O destinatário vê: "Construtora ABC &lt;noreply@dominio.com&gt;"
     * em vez do endereço técnico puro.
     * Display name = companyName ?? systemName ?? "Notificações".
     */
    private InternetAddress senderAddress(SystemSettings s) {
        String email = mailFrom(s);
        String name = s.getCompanyName() != null && !s.getCompanyName().isBlank()
                ? s.getCompanyName()
                : (s.getSystemName() != null && !s.getSystemName().isBlank()
                        ? s.getSystemName()
                        : "Notificações");
        try {
            return new InternetAddress(email, name, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            try { return new InternetAddress(email); }
            catch (jakarta.mail.internet.AddressException ex) {
                return new InternetAddress();
            }
        }
    }

    /** Envolve o conteúdo num layout HTML com a identidade do sistema. */
    private String template(String title, String innerHtml) {
        SystemSettings s = settingsService.current();
        String primary = orDefault(s.getPrimaryColor(), "#1d4ed8");
        String name = orDefault(s.getSystemName(), "ERP Construtora");
        String company = orDefault(s.getCompanyName(), name);
        return ("""
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:0 auto;\
                border:1px solid #eaeaea;border-radius:8px;overflow:hidden">
                  <div style="background:%s;color:#ffffff;padding:16px 24px;font-size:18px;font-weight:bold">%s</div>
                  <div style="padding:24px;color:#333333;font-size:14px;line-height:1.6">%s</div>
                  <div style="background:#f7f7f7;padding:12px 24px;color:#999999;font-size:12px">\
                %s — mensagem automática, por favor não responda este e-mail.</div>
                </div>
                """).formatted(esc(primary), esc(name), innerHtml, esc(company));
    }

    private String recipientOf(Installment i) {
        String email = i.getSale().getClient().getEmail();
        return (email != null && !email.isBlank()) ? email : null;
    }

    private String clientName(Installment i) {
        return i.getSale().getClient().getName();
    }

    private String brl(java.math.BigDecimal v) {
        return "R$ " + (v != null ? v.toPlainString() : "0,00");
    }

    private String orDefault(String v, String def) {
        return (v != null && !v.isBlank()) ? v : def;
    }

    private String blankToEmpty(String v) {
        return (v != null && !v.isBlank()) ? v.trim() : "";
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
