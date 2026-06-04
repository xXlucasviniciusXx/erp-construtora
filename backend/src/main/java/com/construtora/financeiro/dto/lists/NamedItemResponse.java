package com.construtora.financeiro.dto.lists;

import java.util.UUID;

public record NamedItemResponse(UUID id, String name, boolean active, int sortOrder) {}
