package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.role.PermissionResponse;
import com.construtora.financeiro.dto.role.RoleRequest;
import com.construtora.financeiro.dto.role.RoleResponse;
import com.construtora.financeiro.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Perfis de acesso e catálogo de permissões")
@PreAuthorize("hasAuthority('USERS_MANAGE')")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os perfis de acesso")
    public List<RoleResponse> list() {
        return service.findAll();
    }

    @GetMapping("/permissions")
    @Operation(summary = "Catálogo de permissões disponíveis (agrupáveis por módulo)")
    public List<PermissionResponse> permissions() {
        return service.permissionCatalog();
    }

    @PostMapping
    @Operation(summary = "Cria um perfil de acesso")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody RoleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um perfil de acesso")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um perfil de acesso")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
