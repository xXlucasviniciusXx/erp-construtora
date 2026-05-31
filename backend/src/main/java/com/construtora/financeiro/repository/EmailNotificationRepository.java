package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, UUID> {
}
