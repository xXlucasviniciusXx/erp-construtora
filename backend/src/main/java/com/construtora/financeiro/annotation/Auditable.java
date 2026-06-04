package com.construtora.financeiro.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de service para auditoria automática via AOP.
 * O {@link com.construtora.financeiro.aspect.AuditAspect} registra em
 * {@code audit_logs} após a execução com sucesso.
 *
 * <pre>{@code
 * @Auditable(action = "SALE_CREATE", entity = "property_sales")
 * public SaleResponse create(SaleRequest r) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /** Código da ação (ex.: "SALE_CREATE", "RECONCILIATION_DONE"). */
    String action();
    /** Nome da tabela/entidade afetada. */
    String entity();
}
