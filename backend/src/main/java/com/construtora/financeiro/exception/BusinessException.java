package com.construtora.financeiro.exception;

/** Erro de regra de negócio (HTTP 400/409). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
