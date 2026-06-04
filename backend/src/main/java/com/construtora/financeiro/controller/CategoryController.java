package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.category.CategoryRequest;
import com.construtora.financeiro.dto.category.CategoryResponse;
import com.construtora.financeiro.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Categorias financeiras (plano de contas)")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista categorias (grupo → item)")
    @PreAuthorize("hasAuthority('CONTAS_PAGAR_VIEW')")
    public List<CategoryResponse> list() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Cria categoria")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza categoria")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove categoria")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
