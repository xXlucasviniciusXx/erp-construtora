package com.construtora.financeiro.dto.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmailNotificationResponse(
        UUID id,
        String recipient,
        String subject,
        String body,
        String eventType,
        String status,
        String error,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt
) {}
