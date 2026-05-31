package com.construtora.financeiro.service;

import com.construtora.financeiro.model.EmailNotification;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.repository.EmailNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Centraliza o envio de e-mails transacionais. Toda notificação é persistida
 * em {@code email_notifications}; o envio SMTP só ocorre quando {@code app.mail.enabled=true}.
 * No POC (desligado) a mensagem fica registrada com status PENDING e é logada.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailNotificationRepository repository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String from;

    public NotificationService(EmailNotificationRepository repository,
                               ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${app.mail.enabled:false}") boolean mailEnabled,
                               @Value("${app.mail.from}") String from) {
        this.repository = repository;
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.from = from;
    }

    public void notifyOverdue(Installment installment) {
        String to = recipientOf(installment);
        if (to == null) return;
        String subject = "Parcela em atraso #" + installment.getNumber();
        String body = "Olá " + installment.getSale().getClient().getName()
                + ", identificamos que a parcela " + installment.getNumber()
                + " no valor de R$ " + installment.getAmount()
                + " com vencimento em " + installment.getDueDate() + " encontra-se em atraso.";
        send(to, subject, body, "PAYMENT_OVERDUE");
    }

    public void notifyPaymentConfirmed(Installment installment) {
        String to = recipientOf(installment);
        if (to == null) return;
        String subject = "Pagamento confirmado — parcela #" + installment.getNumber();
        String body = "Recebemos o pagamento da parcela " + installment.getNumber()
                + " no valor de R$ " + installment.getAmount() + ". Obrigado!";
        send(to, subject, body, "PAYMENT_CONFIRMED");
    }

    public void notifySaleCreated(PropertySale sale) {
        String to = sale.getClient().getEmail();
        if (to == null || to.isBlank()) return;
        String subject = "Venda registrada";
        String body = "Olá " + sale.getClient().getName()
                + ", sua compra foi registrada no valor de R$ " + sale.getTotalValue()
                + " em " + sale.getInstallmentsCount() + " parcelas.";
        send(to, subject, body, "SALE_CREATED");
    }

    public void notifyContractGenerated(PropertySale sale) {
        String to = sale.getClient().getEmail();
        if (to == null || to.isBlank()) return;
        send(to, "Contrato gerado",
                "O contrato referente à sua compra foi gerado e está disponível.", "CONTRACT_GENERATED");
    }

    private String recipientOf(Installment installment) {
        String email = installment.getSale().getClient().getEmail();
        return (email != null && !email.isBlank()) ? email : null;
    }

    private void send(String to, String subject, String body, String eventType) {
        EmailNotification record = new EmailNotification();
        record.setRecipient(to);
        record.setSubject(subject);
        record.setBody(body);
        record.setEventType(eventType);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            log.info("[E-MAIL SIMULADO] para={} assunto='{}' evento={}", to, subject, eventType);
            record.setStatus("PENDING");
            repository.save(record);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            record.setStatus("SENT");
            record.setSentAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}", to, e);
            record.setStatus("FAILED");
            record.setError(e.getMessage());
        }
        repository.save(record);
    }
}
