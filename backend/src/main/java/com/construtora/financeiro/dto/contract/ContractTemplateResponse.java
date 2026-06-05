package com.construtora.financeiro.dto.contract;

import com.construtora.financeiro.model.ContractTemplate;

import java.util.UUID;

public record ContractTemplateResponse(
        UUID id,
        String kind,
        String name,
        String body,
        UUID developmentId,
        boolean isDefault,
        boolean active
) {
    public static ContractTemplateResponse from(ContractTemplate t) {
        return new ContractTemplateResponse(t.getId(), t.getKind(), t.getName(), t.getBody(),
                t.getDevelopmentId(), t.isDefault(), t.isActive());
    }
}
