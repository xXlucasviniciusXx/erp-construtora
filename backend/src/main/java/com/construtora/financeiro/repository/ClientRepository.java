package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByDocument(String document);
    boolean existsByDocument(String document);
    Page<Client> findByNameContainingIgnoreCaseOrDocumentContaining(String name, String document, Pageable pageable);
}
