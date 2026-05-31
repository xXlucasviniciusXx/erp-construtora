package com.construtora.financeiro.dto.bank;

import com.construtora.financeiro.model.enums.FileFormat;
import com.construtora.financeiro.model.enums.ImportStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImportResultResponse(
        UUID id,
        UUID bankAccountId,
        String fileName,
        FileFormat fileFormat,
        ImportStatus status,
        int totalRows,
        int importedRows,
        String errorMessage,
        OffsetDateTime createdAt
) {}
