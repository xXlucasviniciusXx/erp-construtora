package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.development.DevelopmentRequest;
import com.construtora.financeiro.dto.development.DevelopmentResponse;
import com.construtora.financeiro.service.DevelopmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/developments")
@Tag(name = "Developments", description = "Empreendimentos (nível 1 da hierarquia)")
public class DevelopmentController {

    private final DevelopmentService service;

    public DevelopmentController(DevelopmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista empreendimentos (com valores derivados)")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_VIEW')")
    public List<DevelopmentResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha empreendimento")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_VIEW')")
    public DevelopmentResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria empreendimento")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    @ResponseStatus(HttpStatus.CREATED)
    public DevelopmentResponse create(@Valid @RequestBody DevelopmentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza empreendimento")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    public DevelopmentResponse update(@PathVariable UUID id, @Valid @RequestBody DevelopmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove empreendimento")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
