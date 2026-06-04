package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.auth.AuthResponse;
import com.construtora.financeiro.dto.auth.LoginRequest;
import com.construtora.financeiro.dto.auth.RefreshRequest;
import com.construtora.financeiro.security.AppUserDetails;
import com.construtora.financeiro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "Login com e-mail e senha, retorna JWT + refresh token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Emite um novo par de tokens (access + refresh) a partir de um refresh token válido.
     * Não requer JWT — o refresh token é a credencial.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token usando o refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Revoga todos os refresh tokens do usuário autenticado (logout seguro).
     * O JWT em si não pode ser revogado (stateless), mas sem refresh token o usuário
     * precisará fazer login novamente após a expiração do JWT.
     */
    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token do usuário (logout)")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AppUserDetails principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
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
