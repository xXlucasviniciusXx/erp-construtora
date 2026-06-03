-- Configuração de SMTP editável pela tela de Configurações (antes era só por
-- variáveis de ambiente). As credenciais ficam em system_settings.

ALTER TABLE system_settings
    ADD COLUMN mail_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN mail_host          VARCHAR(200),
    ADD COLUMN mail_port          INTEGER      DEFAULT 587,
    ADD COLUMN mail_username      VARCHAR(200),
    ADD COLUMN mail_password      VARCHAR(300),
    ADD COLUMN mail_from          VARCHAR(200),
    ADD COLUMN mail_reminder_days INTEGER      NOT NULL DEFAULT 3;
