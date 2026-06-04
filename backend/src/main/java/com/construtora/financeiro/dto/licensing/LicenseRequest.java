package com.construtora.financeiro.dto.licensing;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record LicenseRequest(
        @NotBlank String plan,
        @NotBlank String status,
        LocalDate startDate,
        LocalDate endDate,
        Integer periodMonths,
        Integer maxUsers,
        String notes
) {}
