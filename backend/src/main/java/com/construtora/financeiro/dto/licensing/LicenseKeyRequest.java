package com.construtora.financeiro.dto.licensing;

import jakarta.validation.constraints.NotBlank;

public record LicenseKeyRequest(
        @NotBlank String key
) {}
