package com.construtora.financeiro.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para testes de integração com PostgreSQL real (Testcontainers).
 * O Flyway aplica todas as migrations V1–V16 automaticamente no startup.
 *
 * <p>O container é compartilhado entre todos os testes da mesma JVM (Ryuk cuida
 * do cleanup ao final). Cada teste deve limpar seus dados ou usar {@code @Transactional}.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("construtora_test")
                    .withUsername("test")
                    .withPassword("test");
}
