package com.construtora.financeiro.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Acesso ao usuário autenticado no contexto de segurança atual. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return Optional.of(details.getId());
        }
        return Optional.empty();
    }

    /** Nome de usuário (login/e-mail) autenticado, se houver. */
    public static Optional<String> currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            return Optional.of(auth.getName());
        }
        return Optional.empty();
    }
}
