package com.construtora.financeiro.aspect;

import com.construtora.financeiro.annotation.Auditable;
import com.construtora.financeiro.service.AuditService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Intercepts methods annotated with {@link Auditable} and records an entry in
 * {@code audit_logs} after successful execution.
 *
 * <p>ID extraction strategy (first match wins):
 * <ol>
 *   <li>Return value has a {@code getId()} method → uses its result.</li>
 *   <li>First method argument has a {@code getId()} method → uses its result.</li>
 *   <li>First argument is a {@link java.util.UUID} → uses its string representation.</li>
 *   <li>Falls back to {@code null}.</li>
 * </ol>
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void audit(JoinPoint jp, Auditable auditable, Object result) {
        Object entityId = extractId(result);
        if (entityId == null && jp.getArgs().length > 0) {
            entityId = extractId(jp.getArgs()[0]);
        }
        String detail = buildDetail(jp, result);
        auditService.log(auditable.action(), auditable.entity(), entityId, detail);
    }

    // ---- helpers ----

    private Object extractId(Object obj) {
        if (obj == null) return null;
        try {
            Method getId = obj.getClass().getMethod("getId");
            return getId.invoke(obj);
        } catch (Exception ignored) { /* não tem getId */ }
        if (obj instanceof java.util.UUID) return obj;
        return null;
    }

    /** Monta um detalhe legível: nome do método + classe do argumento principal. */
    private String buildDetail(JoinPoint jp, Object result) {
        StringBuilder sb = new StringBuilder(jp.getSignature().toShortString());
        if (result != null) {
            try {
                Method getName = result.getClass().getMethod("getName");
                String name = (String) getName.invoke(result);
                if (name != null) sb.append(" → ").append(name);
            } catch (Exception ignored) { /* sem nome */ }
        }
        return sb.toString();
    }
}
