package com.construtora.financeiro.dto.supplier;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String document,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String category,
        String notes,
        boolean active
) {}
