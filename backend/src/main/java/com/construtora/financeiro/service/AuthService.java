package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.auth.AuthResponse;
import com.construtora.financeiro.dto.auth.LoginRequest;
import com.construtora.financeiro.model.RefreshToken;
import com.construtora.financeiro.security.AppUserDetails;
import com.construtora.financeiro.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(principal);
        RefreshToken rt = refreshTokenService.generate(principal.getUser());

        return buildResponse(principal, accessToken, rt.getToken());
    }

    /**
     * Rotaciona o refresh token e emite um novo par de tokens.
     * Transacional: mantém a sessão aberta para inicializar o User (LAZY),
     * sua Role e as permissions ao montar o AppUserDetails e o JWT.
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken newRt = refreshTokenService.rotate(rawRefreshToken);
        AppUserDetails principal = new AppUserDetails(newRt.getUser());
        String accessToken = jwtService.generateToken(principal);
        return buildResponse(principal, accessToken, newRt.getToken());
    }

    /** Revoga todos os refresh tokens do usuário (logout). */
    public void logout(AppUserDetails principal) {
        refreshTokenService.revokeAll(principal.getId());
    }

    // ---- helpers ----

    private AuthResponse buildResponse(AppUserDetails principal, String accessToken, String refreshToken) {
        List<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationMs(),
                principal.getId(),
                principal.getUser().getName(),
                principal.getUsername(),
                principal.getUser().getRole().getName(),
                permissions,
                refreshToken);
    }
}
