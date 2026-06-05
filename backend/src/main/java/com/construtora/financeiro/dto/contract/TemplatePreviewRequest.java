package com.construtora.financeiro.dto.contract;

import jakarta.validation.constraints.NotBlank;

/** Corpo de template para pré-visualização com dados de exemplo. */
public record TemplatePreviewRequest(@NotBlank String body) {}
