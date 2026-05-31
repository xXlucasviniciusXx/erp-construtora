package com.construtora.financeiro.exception;

import java.time.OffsetDateTime;
import java.util.Map;

/** Corpo padrão de erro retornado pela API. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {}
