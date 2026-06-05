package com.construtora.financeiro.dto.contract;

import com.construtora.financeiro.model.ContractDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Metadados de um documento arquivado (sem o binário do PDF). */
public record ContractDocumentResponse(
        UUID id,
        UUID saleId,
        String type,
        Integer version,
        String fileName,
        OffsetDateTime generatedAt,
        String generatedBy
) {
    public static ContractDocumentResponse from(ContractDocument d) {
        return new ContractDocumentResponse(d.getId(), d.getSale().getId(), d.getType(),
                d.getVersion(), d.getFileName(), d.getGeneratedAt(), d.getGeneratedBy());
    }
}
