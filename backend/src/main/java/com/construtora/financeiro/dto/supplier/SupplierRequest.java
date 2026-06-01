package com.construtora.financeiro.dto.supplier;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String name,
        String document,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String category,
        String notes,
        Boolean active
) {}
