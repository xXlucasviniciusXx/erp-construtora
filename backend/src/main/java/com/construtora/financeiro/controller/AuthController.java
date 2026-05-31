package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.auth.AuthResponse;
import com.construtora.financeiro.dto.auth.LoginRequest;
import com.construtora.financeiro.security.AppUserDetails;
import com.construtora.financeiro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Autenticação e sessão")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login com e-mail e senha, retorna JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal AppUserDetails principal) {
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(Map.of(
                "id", principal.getId(),
                "name", principal.getUser().getName(),
                "email", principal.getUsername(),
                "role", principal.getUser().getRole().getName(),
                "authorities", authorities));
    }
}
