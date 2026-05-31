package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.auth.AuthResponse;
import com.construtora.financeiro.dto.auth.LoginRequest;
import com.construtora.financeiro.security.AppUserDetails;
import com.construtora.financeiro.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        List<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                principal.getId(),
                principal.getUser().getName(),
                principal.getUsername(),
                principal.getUser().getRole().getName(),
                permissions);
    }
}
