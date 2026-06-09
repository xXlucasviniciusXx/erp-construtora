package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoleId(UUID roleId);

    /** IDs dos empreendimentos vinculados ao usuário (escopo de acesso). */
    @Query("select d.id from User u join u.developments d where u.id = :userId")
    List<UUID> findDevelopmentIds(@Param("userId") UUID userId);
}
