package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.notification.EmailNotificationResponse;
import com.construtora.financeiro.model.EmailNotification;
import com.construtora.financeiro.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Histórico e envio de e-mails")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Histórico de notificações (paginado, filtros status/evento)")
    @PreAuthorize("hasAuthority('NOTIFICACOES_VIEW')")
    public Page<EmailNotificationResponse> list(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String eventType,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return service.list(status, eventType, pageable).map(this::toResponse);
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Reenvia uma notificação")
    @PreAuthorize("hasAuthority('NOTIFICACOES_VIEW')")
    public EmailNotificationResponse resend(@PathVariable UUID id) {
        return toResponse(service.resend(id));
    }

    @PostMapping("/test")
    @Operation(summary = "Envia um e-mail de teste para validar o SMTP")
    @PreAuthorize("hasAuthority('NOTIFICACOES_VIEW')")
    public EmailNotificationResponse test(@RequestParam String to) {
        return toResponse(service.sendTest(to));
    }

    private EmailNotificationResponse toResponse(EmailNotification n) {
        return new EmailNotificationResponse(
                n.getId(), n.getRecipient(), n.getSubject(), n.getBody(), n.getEventType(),
                n.getStatus(), n.getError(), n.getCreatedAt(), n.getSentAt());
    }
}
