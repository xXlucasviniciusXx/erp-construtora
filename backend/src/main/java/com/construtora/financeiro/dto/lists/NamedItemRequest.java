package com.construtora.financeiro.dto.lists;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NamedItemRequest(
        @NotBlank @Size(max = 100) String name,
        boolean active,
        int sortOrder
) {}
