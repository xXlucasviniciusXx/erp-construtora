package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.role.PermissionResponse;
import com.construtora.financeiro.dto.role.RoleRequest;
import com.construtora.financeiro.dto.role.RoleResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.Permission;
import com.construtora.financeiro.model.Role;
import com.construtora.financeiro.repository.PermissionRepository;
import com.construtora.financeiro.repository.RoleRepository;
import com.construtora.financeiro.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gestão dos perfis de acesso (papéis) e do catálogo de permissões.
 * O papel ADMIN é protegido: nunca pode ser editado ou removido, garantindo
 * que sempre exista um acesso total ao sistema.
 */
@Service
@Transactional
public class RoleService {

    private static final String ADMIN = "ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                       UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissionCatalog() {
        return permissionRepository.findAllByOrderByCode().stream()
                .map(this::toPermissionResponse)
                .sorted(Comparator.comparing(PermissionResponse::module).thenComparing(PermissionResponse::action))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse create(RoleRequest r) {
        String name = r.name().trim().toUpperCase();
        roleRepository.findByName(name).ifPresent(x -> {
            throw new BusinessException("Já existe um perfil com esse nome");
        });
        Role role = new Role();
        role.setName(name);
        return toResponse(roleRepository.save(apply(role, r)));
    }

    public RoleResponse update(UUID id, RoleRequest r) {
        Role role = getEntity(id);
        if (ADMIN.equals(role.getName())) {
            throw new BusinessException("O perfil ADMIN é protegido e não pode ser alterado");
        }
        return toResponse(roleRepository.save(apply(role, r)));
    }

    public void delete(UUID id) {
        Role role = getEntity(id);
        if (ADMIN.equals(role.getName())) {
            throw new BusinessException("O perfil ADMIN não pode ser removido");
        }
        if (userRepository.countByRoleId(role.getId()) > 0) {
            throw new BusinessException("Há usuários usando este perfil; troque-os antes de remover");
        }
        roleRepository.delete(role);
    }

    private Role apply(Role role, RoleRequest r) {
        role.setDescription(r.description());
        Set<Permission> perms = new HashSet<>();
        if (r.permissions() != null && !r.permissions().isEmpty()) {
            perms.addAll(permissionRepository.findByCodeIn(r.permissions()));
        }
        role.setPermissions(perms);
        return role;
    }

    private Role getEntity(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Perfil", id));
    }

    private RoleResponse toResponse(Role role) {
        List<String> codes = role.getPermissions().stream().map(Permission::getCode).sorted().toList();
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), codes,
                ADMIN.equals(role.getName()), userRepository.countByRoleId(role.getId()));
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        String code = p.getCode();
        String module;
        String action;
        if (code.endsWith("_VIEW")) {
            module = code.substring(0, code.length() - 5);
            action = "VIEW";
        } else if (code.endsWith("_EDIT")) {
            module = code.substring(0, code.length() - 5);
            action = "EDIT";
        } else {
            module = "SISTEMA";
            action = "MANAGE";
        }
        return new PermissionResponse(code, p.getDescription(), module, action);
    }
}
