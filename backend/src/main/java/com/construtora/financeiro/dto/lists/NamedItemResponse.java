package com.construtora.financeiro.dto.lists;

import java.util.UUID;

public record NamedItemResponse(UUID id, String name, boolean active, int sortOrder, Integer sgsCode) {

    /** Para listas sem código SGS (formas de pagamento, categorias de fornecedor). */
    public NamedItemResponse(UUID id, String name, boolean active, int sortOrder) {
        this(id, name, active, sortOrder, null);
    }
}
