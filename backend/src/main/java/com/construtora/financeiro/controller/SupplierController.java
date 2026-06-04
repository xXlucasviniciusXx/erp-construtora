package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.supplier.SupplierRequest;
import com.construtora.financeiro.dto.supplier.SupplierResponse;
import com.construtora.financeiro.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers", description = "Cadastro de fornecedores")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista/pesquisa fornecedores (paginado)")
    @PreAuthorize("hasAuthority('FORNECEDORES_VIEW')")
    public org.springframework.data.domain.Page<SupplierResponse> list(
            @RequestParam(required = false) String q,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "name") org.springframework.data.domain.Pageable pageable) {
        return service.search(q, pageable);
    }

    @PostMapping
    @Operation(summary = "Cria fornecedor")
    @PreAuthorize("hasAuthority('FORNECEDORES_EDIT')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza fornecedor")
    @PreAuthorize("hasAuthority('FORNECEDORES_EDIT')")
    public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove fornecedor")
    @PreAuthorize("hasAuthority('FORNECEDORES_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
