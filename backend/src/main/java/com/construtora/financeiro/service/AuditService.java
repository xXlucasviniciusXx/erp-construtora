package com.construtora.financeiro.service;

import com.construtora.financeiro.model.AuditLog;
import com.construtora.financeiro.repository.AuditLogRepository;
import com.construtora.financeiro.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registra ações sensíveis em {@code audit_logs}. */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entity, Object entityId, String detail) {
        AuditLog entry = new AuditLog();
        SecurityUtils.currentUserId().ifPresent(entry::setUserId);
        entry.setAction(action);
        entry.setEntity(entity);
        entry.setEntityId(entityId != null ? entityId.toString() : null);
        entry.setDetail(detail);
        repository.save(entry);
    }
}
