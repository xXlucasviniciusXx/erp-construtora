package com.construtora.financeiro.security;

import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolve o escopo de empreendimentos do usuário autenticado.
 *
 * <ul>
 *   <li>Usuários com a permissão {@code ACESSO_GLOBAL_EMPREENDIMENTOS} são
 *       <b>irrestritos</b> ({@link #allowedDevelopmentIds()} retorna vazio).</li>
 *   <li>Demais usuários ficam <b>restritos</b> aos empreendimentos vinculados
 *       (tabela {@code user_developments}); sem vínculo = conjunto vazio = não
 *       enxerga nada (padrão seguro).</li>
 * </ul>
 */
@Service
public class DevelopmentScopeService {

    public static final String GLOBAL_ACCESS = "ACESSO_GLOBAL_EMPREENDIMENTOS";

    private final UserRepository userRepository;

    public DevelopmentScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * IDs de empreendimentos permitidos ao usuário atual.
     * {@code Optional.empty()} = irrestrito (vê tudo).
     * {@code Optional.of(set)} = restrito ao conjunto (pode ser vazio = nada).
     */
    @Transactional(readOnly = true)
    public Optional<Set<UUID>> allowedDevelopmentIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.of(Set.of());   // sem autenticação → nada (defensivo)
        }
        boolean global = auth.getAuthorities().stream()
                .anyMatch(a -> GLOBAL_ACCESS.equals(a.getAuthority()));
        if (global) {
            return Optional.empty();        // irrestrito
        }
        UUID userId = SecurityUtils.currentUserId().orElse(null);
        if (userId == null) {
            return Optional.of(Set.of());
        }
        return Optional.of(new HashSet<>(userRepository.findDevelopmentIds(userId)));
    }

    /** {@code true} se o usuário tem escopo restrito (não vê tudo). */
    public boolean isRestricted() {
        return allowedDevelopmentIds().isPresent();
    }

    /**
     * Conjunto de IDs permitidos, ou {@code null} quando irrestrito — formato
     * conveniente para passar a queries ({@code :devIds is null or ... in :devIds}).
     */
    public Set<UUID> scopeOrNull() {
        return allowedDevelopmentIds().orElse(null);
    }

    /** Verifica se o usuário pode acessar um empreendimento específico. */
    public boolean canAccess(UUID developmentId) {
        Optional<Set<UUID>> allowed = allowedDevelopmentIds();
        return allowed.isEmpty() || (developmentId != null && allowed.get().contains(developmentId));
    }

    /**
     * Garante o acesso a um empreendimento ou lança 404 (não revela existência
     * de recursos fora do escopo — anti-IDOR). {@code resource}/{@code id}
     * compõem a mensagem.
     */
    public void requireAccess(UUID developmentId, String resource, Object id) {
        if (!canAccess(developmentId)) {
            throw ResourceNotFoundException.of(resource, id);
        }
    }

    /** UUID-sentinela que nunca casa com um empreendimento real (evita {@code IN ()} vazio). */
    private static final UUID NONE = new UUID(0L, 0L);

    /** Par (irrestrito, devIds) para queries no formato {@code :unrestricted = true or x in :devIds}. */
    public record QueryScope(boolean unrestricted, Collection<UUID> devIds) {}

    public QueryScope queryScope() {
        Optional<Set<UUID>> allowed = allowedDevelopmentIds();
        if (allowed.isEmpty()) {
            return new QueryScope(true, java.util.List.of(NONE));
        }
        Set<UUID> set = allowed.get();
        return new QueryScope(false, set.isEmpty() ? java.util.List.of(NONE) : set);
    }

    /** Filtra uma coleção pelo escopo (mantém tudo se irrestrito). */
    public <T> java.util.List<T> filter(Collection<T> items, java.util.function.Function<T, UUID> devIdOf) {
        Optional<Set<UUID>> allowed = allowedDevelopmentIds();
        if (allowed.isEmpty()) return new java.util.ArrayList<>(items);
        Set<UUID> set = allowed.get();
        return items.stream().filter(i -> {
            UUID d = devIdOf.apply(i);
            return d != null && set.contains(d);
        }).toList();
    }
}
