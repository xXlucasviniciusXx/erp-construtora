-- ============================================================
--  V20: Modelos de contrato passam a guardar FRAGMENTO HTML
--  (o esqueleto XHTML + CSS e a normalização passam para o backend).
--  Permite o editor visual (WYSIWYG) sem o usuário ver código.
-- ============================================================

-- 1. Reescreve os 2 modelos padrão como fragmentos limpos.
--    O token {{parcelas_tabela}} agora é uma tabela autocontida (inserível
--    em qualquer ponto), então não fica mais dentro de um <table> manual.

UPDATE contract_templates SET body =
'<h1>CONTRATO DE COMPRA E VENDA DE IMÓVEL</h1>
<p class="meta">Contrato nº {{numero_contrato}}</p>
<p>Por este instrumento particular, <strong>{{empresa}}</strong> (VENDEDORA) e o(a) comprador(a) abaixo qualificado(a) ajustam a compra e venda do imóvel descrito a seguir.</p>
<div class="section"><strong>COMPRADOR(A)</strong><br/>Nome/Razão Social: {{cliente_nome}}<br/>CPF/CNPJ: {{cliente_documento}} &#160;&#160; RG/IE: {{cliente_rg_ie}}<br/>Endereço: {{cliente_endereco}}<br/>Estado civil: {{cliente_estado_civil}} &#160;&#160; Profissão: {{cliente_profissao}}<br/>E-mail: {{cliente_email}} &#160;&#160; Telefone: {{cliente_telefone}}</div>
<div class="section"><strong>IMÓVEL</strong><br/>Empreendimento: {{empreendimento}}<br/>Quadra: {{quadra}} &#160; Lote: {{lote}} &#160; Unidade: {{unidade}}<br/>Matrícula: {{matricula}}<br/>Endereço: {{imovel_endereco}}<br/>Área total: {{area_total}} m² &#160; Área construída: {{area_construida}} m²</div>
<div class="section"><strong>CONDIÇÕES DE PAGAMENTO</strong><br/>Valor total: R$ {{valor_total}}<br/>Entrada: R$ {{entrada}}<br/>Parcelas: {{parcelas_qtd}} &#160; Primeiro vencimento: {{primeiro_vencimento}}<br/>Forma de pagamento: {{forma_pagamento}} &#160; Índice de correção: {{indice_correcao}}</div>
<div class="section"><strong>PARCELAS</strong></div>
{{parcelas_tabela}}
<p class="section">{{clausulas_extras}}</p>
<p class="section">Local e data: ________________________, {{data_hoje}}.</p>
<div class="signatures"><span class="sig">VENDEDORA<br/>{{empresa}}</span> &#160;&#160;&#160;&#160;&#160;&#160; <span class="sig">COMPRADOR(A)<br/>{{cliente_nome}}</span></div>'
WHERE kind = 'CONTRACT' AND is_default = TRUE;

UPDATE contract_templates SET body =
'<h1>DISTRATO DE CONTRATO DE COMPRA E VENDA</h1>
<p class="meta">Refer. contrato nº {{numero_contrato}}</p>
<p>Por este instrumento particular, <strong>{{empresa}}</strong> (VENDEDORA) e {{cliente_nome}} (COMPRADOR(A)), CPF/CNPJ {{cliente_documento}}, resolvem de comum acordo rescindir o contrato de compra e venda do imóvel {{empreendimento}} — Quadra {{quadra}}, Lote {{lote}}.</p>
<div class="section"><strong>CONDIÇÕES DO DISTRATO</strong><br/>Data do distrato: {{distrato_data}}<br/>Valor total do contrato: R$ {{valor_total}}<br/>Valor a devolver ao(à) comprador(a): R$ {{distrato_devolucao}}<br/>Valor retido pela vendedora: R$ {{distrato_retido}}</div>
<div class="section"><strong>MOTIVO</strong><br/>{{distrato_motivo}}</div>
<p class="section">Com a assinatura deste distrato, as partes dão-se mútua, geral, plena e irrevogável quitação quanto ao contrato ora rescindido, nada mais tendo a reclamar uma da outra.</p>
<p class="section">Local e data: ________________________, {{data_hoje}}.</p>
<div class="signatures"><span class="sig">VENDEDORA<br/>{{empresa}}</span> &#160;&#160;&#160;&#160;&#160;&#160; <span class="sig">COMPRADOR(A)<br/>{{cliente_nome}}</span></div>'
WHERE kind = 'DISTRATO' AND is_default = TRUE;

-- 2. Qualquer outro modelo (customizado) ainda em documento completo: extrai o
--    conteúdo interno do <body> para virar fragmento. (Postgres: '.' casa
--    quebras de linha por padrão.)
UPDATE contract_templates
   SET body = regexp_replace(
                regexp_replace(body, '</body>.*$', ''),  -- remove sufixo a partir de </body>
                '^.*<body[^>]*>', '')                     -- remove prefixo até <body ...>
 WHERE body ILIKE '%<body%';
