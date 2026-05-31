package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.user.UserRequest;
import com.construtora.financeiro.dto.user.UserResponse;
import com.construtora.financeiro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gestão de usuários e perfis (ADMIN)")
@PreAuthorize("hasAuthority('USERS_MANAGE')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Lista usuários")
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um usuário")
    public UserResponse get(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria usuário")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza usuário")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa usuário")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }
}
