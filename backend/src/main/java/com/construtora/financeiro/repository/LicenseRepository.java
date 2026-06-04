package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {
}
