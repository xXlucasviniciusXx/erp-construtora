-- ============================================================
--  V19: Contratos — numeração, distrato, templates e arquivo
-- ============================================================

-- ---- 1. Número de contrato sequencial em property_sales ----
ALTER TABLE property_sales ADD COLUMN contract_number VARCHAR(30);

CREATE SEQUENCE IF NOT EXISTS contract_number_seq START 1;

-- Backfill determinístico das vendas existentes (ordem por data da venda / criação)
WITH ordered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY sale_date, created_at) AS rn
    FROM property_sales
)
UPDATE property_sales s
   SET contract_number = 'CT-' || LPAD(o.rn::text, 6, '0')
  FROM ordered o
 WHERE s.id = o.id;

-- Posiciona a sequência após o último número atribuído
SELECT setval('contract_number_seq', GREATEST((SELECT COUNT(*) FROM property_sales), 1));

CREATE UNIQUE INDEX idx_property_sales_contract_number ON property_sales(contract_number);

-- ---- 2. Distrato (rescisão amigável) — campos em property_sales ----
ALTER TABLE property_sales ADD COLUMN distrato_date            DATE;
ALTER TABLE property_sales ADD COLUMN distrato_reason          TEXT;
ALTER TABLE property_sales ADD COLUMN distrato_refund_amount   NUMERIC(15,2);
ALTER TABLE property_sales ADD COLUMN distrato_retained_amount NUMERIC(15,2);

-- ---- 3. Templates de contrato/distrato persistidos e editáveis ----
CREATE TABLE contract_templates (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    kind        VARCHAR(20)  NOT NULL DEFAULT 'CONTRACT',   -- CONTRACT | DISTRATO
    name        VARCHAR(120) NOT NULL,
    body        TEXT         NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_contract_templates_kind ON contract_templates(kind);
-- No máximo um template padrão por tipo
CREATE UNIQUE INDEX idx_contract_templates_default ON contract_templates(kind) WHERE is_default;

-- Template padrão de CONTRATO (mesmo layout do gerador anterior, agora com tokens {{...}})
INSERT INTO contract_templates (kind, name, is_default, body) VALUES ('CONTRACT', 'Compra e Venda (padrão)', TRUE,
'<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><style>
  body { font-family: sans-serif; font-size: 12px; color: #111; }
  h1 { font-size: 18px; text-align: center; }
  .meta { text-align: center; font-size: 11px; color: #555; }
  table { width: 100%; border-collapse: collapse; margin-top: 8px; }
  th, td { border: 1px solid #999; padding: 4px; text-align: left; }
  .section { margin-top: 16px; }
  .signatures { margin-top: 60px; }
  .sig { display: inline-block; width: 45%; text-align: center; border-top: 1px solid #000; }
</style></head>
<body>
  <h1>CONTRATO DE COMPRA E VENDA DE IM&#211;VEL</h1>
  <p class="meta">Contrato n&#186; {{numero_contrato}}</p>
  <p>Por este instrumento particular, <strong>{{empresa}}</strong> (VENDEDORA) e o(a) comprador(a)
     abaixo qualificado(a) ajustam a compra e venda do im&#243;vel descrito a seguir.</p>

  <div class="section">
    <strong>COMPRADOR(A)</strong><br/>
    Nome/Raz&#227;o Social: {{cliente_nome}}<br/>
    CPF/CNPJ: {{cliente_documento}} &#160;&#160; RG/IE: {{cliente_rg_ie}}<br/>
    Endere&#231;o: {{cliente_endereco}}<br/>
    Estado civil: {{cliente_estado_civil}} &#160;&#160; Profiss&#227;o: {{cliente_profissao}}<br/>
    E-mail: {{cliente_email}} &#160;&#160; Telefone: {{cliente_telefone}}
  </div>

  <div class="section">
    <strong>IM&#211;VEL</strong><br/>
    Empreendimento: {{empreendimento}}<br/>
    Quadra: {{quadra}} &#160; Lote: {{lote}} &#160; Unidade: {{unidade}}<br/>
    Matr&#237;cula: {{matricula}}<br/>
    Endere&#231;o: {{imovel_endereco}}<br/>
    &#193;rea total: {{area_total}} m&#178; &#160; &#193;rea constru&#237;da: {{area_construida}} m&#178;
  </div>

  <div class="section">
    <strong>CONDI&#199;&#213;ES DE PAGAMENTO</strong><br/>
    Valor total: R$ {{valor_total}}<br/>
    Entrada: R$ {{entrada}}<br/>
    Parcelas: {{parcelas_qtd}} &#160; Primeiro vencimento: {{primeiro_vencimento}}<br/>
    Forma de pagamento: {{forma_pagamento}} &#160; &#205;ndice de corre&#231;&#227;o: {{indice_correcao}}
  </div>

  <div class="section">
    <strong>PARCELAS</strong>
    <table>
      <tr><th>N&#186;</th><th>Valor</th><th>Vencimento</th></tr>
      {{parcelas_tabela}}
    </table>
  </div>

  <p class="section">{{clausulas_extras}}</p>

  <p class="section">Local e data: ________________________, {{data_hoje}}.</p>

  <div class="signatures">
    <span class="sig">VENDEDORA<br/>{{empresa}}</span>
    &#160;&#160;&#160;&#160;&#160;&#160;
    <span class="sig">COMPRADOR(A)<br/>{{cliente_nome}}</span>
  </div>
</body></html>');

-- Template padrão de DISTRATO
INSERT INTO contract_templates (kind, name, is_default, body) VALUES ('DISTRATO', 'Distrato (padrão)', TRUE,
'<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><style>
  body { font-family: sans-serif; font-size: 12px; color: #111; }
  h1 { font-size: 18px; text-align: center; }
  .meta { text-align: center; font-size: 11px; color: #555; }
  .section { margin-top: 16px; }
  .signatures { margin-top: 60px; }
  .sig { display: inline-block; width: 45%; text-align: center; border-top: 1px solid #000; }
</style></head>
<body>
  <h1>DISTRATO DE CONTRATO DE COMPRA E VENDA</h1>
  <p class="meta">Refer. contrato n&#186; {{numero_contrato}}</p>
  <p>Por este instrumento particular, <strong>{{empresa}}</strong> (VENDEDORA) e {{cliente_nome}}
     (COMPRADOR(A)), CPF/CNPJ {{cliente_documento}}, resolvem de comum acordo rescindir o contrato de
     compra e venda do im&#243;vel {{empreendimento}} &#8212; Quadra {{quadra}}, Lote {{lote}}.</p>

  <div class="section">
    <strong>CONDI&#199;&#213;ES DO DISTRATO</strong><br/>
    Data do distrato: {{distrato_data}}<br/>
    Valor total do contrato: R$ {{valor_total}}<br/>
    Valor a devolver ao(&#224;) comprador(a): R$ {{distrato_devolucao}}<br/>
    Valor retido pela vendedora: R$ {{distrato_retido}}
  </div>

  <div class="section">
    <strong>MOTIVO</strong><br/>
    {{distrato_motivo}}
  </div>

  <p class="section">Com a assinatura deste distrato, as partes d&#227;o-se m&#250;tua, geral, plena e
     irrevog&#225;vel quita&#231;&#227;o quanto ao contrato ora rescindido, nada mais tendo a reclamar uma da outra.</p>

  <p class="section">Local e data: ________________________, {{data_hoje}}.</p>

  <div class="signatures">
    <span class="sig">VENDEDORA<br/>{{empresa}}</span>
    &#160;&#160;&#160;&#160;&#160;&#160;
    <span class="sig">COMPRADOR(A)<br/>{{cliente_nome}}</span>
  </div>
</body></html>');

-- ---- 4. Arquivo/versionamento dos documentos gerados (PDF no banco) ----
CREATE TABLE contract_documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id       UUID         NOT NULL REFERENCES property_sales(id) ON DELETE CASCADE,
    type          VARCHAR(20)  NOT NULL,                 -- CONTRACT | DISTRATO
    version       INT          NOT NULL,
    file_name     VARCHAR(200) NOT NULL,
    pdf_data      BYTEA        NOT NULL,
    generated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    generated_by  VARCHAR(150)
);
CREATE INDEX idx_contract_documents_sale ON contract_documents(sale_id);
