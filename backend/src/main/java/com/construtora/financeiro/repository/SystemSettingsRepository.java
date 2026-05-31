package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, UUID> {
}
