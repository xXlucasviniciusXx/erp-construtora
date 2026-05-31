package com.construtora.financeiro.bootstrap;

import com.construtora.financeiro.model.Role;
import com.construtora.financeiro.model.User;
import com.construtora.financeiro.repository.RoleRepository;
import com.construtora.financeiro.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o usuário ADMIN inicial a partir das variáveis de ambiente,
 * evitando senha em texto puro nas migrations. Idempotente.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role ADMIN não encontrada (rode as migrations)"));

        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setActive(true);
        userRepository.save(admin);

        log.info("Usuário ADMIN inicial criado: {}", adminEmail);
    }
}
