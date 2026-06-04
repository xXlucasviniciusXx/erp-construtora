package com.construtora.financeiro.service;

import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.model.RefreshToken;
import com.construtora.financeiro.model.User;
import com.construtora.financeiro.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    /** Validade do refresh token: 30 dias. */
    private static final int REFRESH_DAYS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    /** Gera e persiste um novo refresh token para o usuário. */
    public RefreshToken generate(User user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        rt.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_DAYS));
        return repository.save(rt);
    }

    /**
     * Valida um refresh token e realiza a rotação (revoga o antigo, emite um novo).
     * Retorna o novo RefreshToken.
     */
    public RefreshToken rotate(String rawToken) {
        RefreshToken rt = repository.findByToken(rawToken)
                .orElseThrow(() -> new BusinessException("Refresh token inválido."));

        if (rt.isRevoked()) {
            // Token já foi usado — possível roubo; revogar todos do usuário
            repository.revokeAllByUserId(rt.getUser().getId());
            throw new BusinessException("Refresh token já utilizado. Faça login novamente.");
        }
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expirado. Faça login novamente.");
        }

        // Revogar o token atual e emitir um novo (rotação)
        rt.setRevoked(true);
        repository.save(rt);
        return generate(rt.getUser());
    }

    /** Revoga todos os refresh tokens do usuário (logout). */
    public void revokeAll(UUID userId) {
        repository.revokeAllByUserId(userId);
    }
}
