package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.property.PropertyRequest;
import com.construtora.financeiro.dto.property.PropertyResponse;
import com.construtora.financeiro.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@Tag(name = "Properties", description = "Cadastro de imóveis, lotes e unidades")
public class PropertyController {

    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista imóveis (paginado)")
    @PreAuthorize("hasAuthority('READ')")
    public Page<PropertyResponse> list(@PageableDefault(size = 20, sort = "development") Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha imóvel")
    @PreAuthorize("hasAuthority('READ')")
    public PropertyResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria imóvel")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza imóvel")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    public PropertyResponse update(@PathVariable UUID id, @Valid @RequestBody PropertyRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Inativa/cancela imóvel (status CANCELADO)")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    public PropertyResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove imóvel")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
