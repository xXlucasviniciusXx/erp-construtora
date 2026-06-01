-- ============================================================
--  Bootstrap para PostgreSQL LOCAL (instalacao nativa, ex.: PG 16/17/18).
--  Cria o usuario e o banco usados pelos defaults da aplicacao
--  (construtora/construtora) e habilita a extensao pgcrypto.
--
--  Rode UMA VEZ como superusuario (postgres):
--    psql -U postgres -h localhost -f database/init-local.sql
--
--  Depois basta subir o backend com os defaults:
--    cd backend && mvn spring-boot:run
-- ============================================================

-- Usuario da aplicacao (idempotente)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'construtora') THEN
    CREATE ROLE construtora LOGIN PASSWORD 'construtora';
  END IF;
END $$;

-- Banco da aplicacao (CREATE DATABASE nao roda em bloco/transacao;
-- se ja existir, ignore o erro "already exists").
\set ON_ERROR_STOP off
CREATE DATABASE construtora OWNER construtora;
\set ON_ERROR_STOP on

-- Habilita pgcrypto no banco da aplicacao. Como o pgcrypto passa a existir,
-- o "CREATE EXTENSION IF NOT EXISTS pgcrypto" da migration V1 vira no-op e
-- NAO exige superusuario em tempo de execucao.
\connect construtora
CREATE EXTENSION IF NOT EXISTS pgcrypto;
GRANT ALL ON SCHEMA public TO construtora;
