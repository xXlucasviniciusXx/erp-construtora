package com.construtora.financeiro.service.contract;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.SystemSettings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Substitui os tokens {@code {{...}}} de um corpo de template (XHTML) pelos
 * dados da venda/cliente/imóvel/empresa, produzindo XHTML BEM-FORMADO (XML) —
 * toda saída usada pelo gerador de PDF (Flying Saucer) precisa ser XML válido:
 *  - valores injetados são escapados ({@link #esc}) para neutralizar &amp; &lt; &gt;
 *  - {@code {{parcelas_tabela}}} é gerado internamente já bem-formado.
 *
 * <p>Tokens não reconhecidos são removidos (substituídos por vazio) para não
 * vazar {@code {{algo}}} no documento final.
 */
@Service
public class ContractRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Renderiza o corpo do template para uma venda concreta. */
    public String render(PropertySale sale, SystemSettings settings, String body) {
        var client = sale.getClient();
        var lot = sale.getLot();
        var block = lot.getBlock();
        var development = block.getDevelopment();
        String company = settings.getCompanyName() != null ? settings.getCompanyName() : settings.getSystemName();

        StringBuilder rows = new StringBuilder();
        for (Installment i : sale.getInstallments()) {
            rows.append("<tr><td>").append(i.getNumber()).append("</td><td>R$ ")
                    .append(i.getAmount()).append("</td><td>").append(i.getDueDate().format(DATE))
                    .append("</td></tr>");
        }

        Map<String, String> t = new LinkedHashMap<>();
        t.put("empresa", esc(company));
        t.put("numero_contrato", esc(sale.getContractNumber()));
        // Comprador
        t.put("cliente_nome", esc(client.getName()));
        t.put("cliente_documento", esc(client.getDocument()));
        t.put("cliente_rg_ie", esc(client.getStateRegistration()));
        t.put("cliente_endereco", esc(client.getAddress()));
        t.put("cliente_estado_civil", esc(client.getMaritalStatus()));
        t.put("cliente_profissao", esc(client.getOccupation()));
        t.put("cliente_email", esc(client.getEmail()));
        t.put("cliente_telefone", esc(client.getPhone()));
        // Imóvel
        t.put("empreendimento", esc(development.getName()));
        t.put("quadra", esc(block.getName()));
        t.put("lote", esc(lot.getName()));
        t.put("unidade", esc(lot.getUnit()));
        t.put("matricula", esc(lot.getRegistration()));
        t.put("imovel_endereco", esc(lot.getAddress()));
        t.put("area_total", esc(lot.getTotalArea()));
        t.put("area_construida", esc(lot.getBuiltArea()));
        // Condições
        t.put("valor_total", esc(sale.getTotalValue()));
        t.put("entrada", esc(sale.getDownPayment()));
        t.put("parcelas_qtd", esc(sale.getInstallmentsCount()));
        t.put("primeiro_vencimento", esc(sale.getFirstDueDate().format(DATE)));
        t.put("forma_pagamento", esc(sale.getPaymentMethod()));
        t.put("indice_correcao", esc(sale.getCorrectionIndex()));
        t.put("clausulas_extras", esc(lot.getContractExtra()));
        t.put("data_hoje", LocalDate.now().format(DATE));
        // Distrato
        t.put("distrato_data", esc(sale.getDistratoDate() != null ? sale.getDistratoDate().format(DATE) : null));
        t.put("distrato_motivo", esc(sale.getDistratoReason()));
        t.put("distrato_devolucao", esc(nz(sale.getDistratoRefundAmount())));
        t.put("distrato_retido", esc(nz(sale.getDistratoRetainedAmount())));
        // Tabela de parcelas (HTML já bem-formado, NÃO escapar)
        t.put("parcelas_tabela", rows.toString());

        return substitute(body, t);
    }

    /** Substitui {{token}} pelos valores; tokens desconhecidos viram string vazia. */
    private String substitute(String body, Map<String, String> tokens) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}").matcher(body);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = tokens.get(m.group(1));
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(value != null ? value : ""));
        }
        m.appendTail(out);
        return out.toString();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Null-safe + escape de caracteres especiais para XML bem-formado. */
    private String esc(Object v) {
        if (v == null) return "-";
        String s = v.toString();
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
