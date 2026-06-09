-- Fase B — Fluxo de aprovação de Contas a Receber
-- Ciclo de aprovação separado do status operacional (OPEN/RECEIVED/OVERDUE/CANCELLED).
-- DEFAULT 'APPROVED' garante que linhas EXISTENTES (legado) permaneçam aprovadas e não bloqueiem dados.
-- Novas linhas criadas pela aplicação setam PENDING explicitamente no código.

ALTER TABLE accounts_receivable
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
        CHECK (approval_status IN ('PENDING','APPROVED','REJECTED'));

ALTER TABLE accounts_receivable
    ADD COLUMN approved_by UUID REFERENCES users(id);

ALTER TABLE accounts_receivable
    ADD COLUMN approved_at TIMESTAMPTZ;

ALTER TABLE accounts_receivable
    ADD COLUMN rejection_reason TEXT;
