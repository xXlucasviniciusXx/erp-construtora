package com.construtora.financeiro.service.contract;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.SystemSettings;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Monta o XHTML do contrato a partir dos dados de venda, cliente, imóvel,
 * parcelas e da empresa. Produz XHTML BEM-FORMADO (XML) — toda saída usada pelo
 * gerador de PDF (Flying Saucer) precisa ser XML válido:
 *  - valores injetados são escapados ({@link #esc}) para neutralizar &amp; &lt; &gt;
 *  - espaços não separáveis usam a referência numérica &amp;#160; (e NÃO &amp;nbsp;,
 *    que é entidade HTML e quebra o parser XML).
 *
 * TODO: suportar múltiplos modelos de contrato persistidos em banco
 *       (tabela contract_templates) e edição pelo COMERCIAL/ADMIN.
 */
@Service
public class ContractTemplateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String render(PropertySale sale, SystemSettings settings) {
        var client = sale.getClient();
        var lot = sale.getLot();
        var block = lot.getBlock();
        var development = block.getDevelopment();
        String company = esc(settings.getCompanyName() != null ? settings.getCompanyName() : settings.getSystemName());

        StringBuilder installments = new StringBuilder();
        for (Installment i : sale.getInstallments()) {
            installments.append("<tr><td>").append(i.getNumber()).append("</td><td>R$ ")
                    .append(i.getAmount()).append("</td><td>").append(i.getDueDate().format(DATE))
                    .append("</td></tr>");
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><style>
                  body { font-family: sans-serif; font-size: 12px; color: #111; }
                  h1 { font-size: 18px; text-align: center; }
                  table { width: 100%%; border-collapse: collapse; margin-top: 8px; }
                  th, td { border: 1px solid #999; padding: 4px; text-align: left; }
                  .section { margin-top: 16px; }
                  .signatures { margin-top: 60px; }
                  .sig { display: inline-block; width: 45%%; text-align: center; border-top: 1px solid #000; }
                </style></head>
                <body>
                  <h1>CONTRATO DE COMPRA E VENDA DE IM&#211;VEL</h1>
                  <p>Por este instrumento particular, <strong>%s</strong> (VENDEDORA) e o(a) comprador(a)
                     abaixo qualificado(a) ajustam a compra e venda do im&#243;vel descrito a seguir.</p>

                  <div class="section">
                    <strong>COMPRADOR(A)</strong><br/>
                    Nome/Raz&#227;o Social: %s<br/>
                    CPF/CNPJ: %s &#160;&#160; RG/IE: %s<br/>
                    Endere&#231;o: %s<br/>
                    Estado civil: %s &#160;&#160; Profiss&#227;o: %s<br/>
                    E-mail: %s &#160;&#160; Telefone: %s
                  </div>

                  <div class="section">
                    <strong>IM&#211;VEL</strong><br/>
                    Empreendimento: %s<br/>
                    Quadra: %s &#160; Lote: %s &#160; Unidade: %s<br/>
                    Matr&#237;cula: %s<br/>
                    Endere&#231;o: %s<br/>
                    &#193;rea total: %s m&#178; &#160; &#193;rea constru&#237;da: %s m&#178;
                  </div>

                  <div class="section">
                    <strong>CONDI&#199;&#213;ES DE PAGAMENTO</strong><br/>
                    Valor total: R$ %s<br/>
                    Entrada: R$ %s<br/>
                    Parcelas: %s &#160; Primeiro vencimento: %s<br/>
                    Forma de pagamento: %s &#160; &#205;ndice de corre&#231;&#227;o: %s
                  </div>

                  <div class="section">
                    <strong>PARCELAS</strong>
                    <table>
                      <tr><th>N&#186;</th><th>Valor</th><th>Vencimento</th></tr>
                      %s
                    </table>
                  </div>

                  <p class="section">%s</p>

                  <p class="section">Local e data: ________________________, %s.</p>

                  <div class="signatures">
                    <span class="sig">VENDEDORA<br/>%s</span>
                    &#160;&#160;&#160;&#160;&#160;&#160;
                    <span class="sig">COMPRADOR(A)<br/>%s</span>
                  </div>
                </body></html>
                """.formatted(
                company,
                esc(client.getName()), esc(client.getDocument()), esc(client.getStateRegistration()),
                esc(client.getAddress()), esc(client.getMaritalStatus()), esc(client.getOccupation()),
                esc(client.getEmail()), esc(client.getPhone()),
                esc(development.getName()), esc(block.getName()), esc(lot.getName()),
                esc(lot.getUnit()), esc(lot.getRegistration()), esc(lot.getAddress()),
                esc(lot.getTotalArea()), esc(lot.getBuiltArea()),
                sale.getTotalValue(), sale.getDownPayment(), sale.getInstallmentsCount(),
                sale.getFirstDueDate().format(DATE), esc(sale.getPaymentMethod()), esc(sale.getCorrectionIndex()),
                installments.toString(),
                esc(lot.getContractExtra()),
                LocalDate.now().format(DATE),
                company, esc(client.getName()));
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
