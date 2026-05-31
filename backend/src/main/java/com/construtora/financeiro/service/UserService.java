package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.user.UserRequest;
import com.construtora.financeiro.dto.user.UserResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.UserMapper;
import com.construtora.financeiro.model.Role;
import com.construtora.financeiro.model.User;
import com.construtora.financeiro.repository.RoleRepository;
import com.construtora.financeiro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, UserMapper mapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    public UserResponse create(UserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Senha é obrigatória na criação do usuário");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(resolveRole(request.role()));
        user.setActive(request.active() == null || request.active());
        return mapper.toResponse(userRepository.save(user));
    }

    public UserResponse update(UUID id, UserRequest request) {
        User user = getEntity(id);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(resolveRole(request.role()));
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return mapper.toResponse(userRepository.save(user));
    }

    public void delete(UUID id) {
        User user = getEntity(id);
        user.setActive(false);   // desativação lógica preserva histórico/auditoria
        userRepository.save(user);
    }

    private User getEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuário", id));
    }

    private Role resolveRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new BusinessException("Perfil inválido: " + name));
    }
}
