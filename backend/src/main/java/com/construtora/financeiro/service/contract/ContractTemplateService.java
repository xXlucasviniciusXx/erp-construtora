package com.construtora.financeiro.service.contract;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.SystemSettings;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Monta o XHTML do contrato a partir dos dados de venda, cliente, imóvel,
 * parcelas e da empresa. Produz XHTML bem-formado para também servir de
 * entrada ao gerador de PDF.
 *
 * TODO: suportar múltiplos modelos de contrato persistidos em banco
 *       (tabela contract_templates) e edição pelo COMERCIAL/ADMIN.
 */
@Service
public class ContractTemplateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String render(PropertySale sale, SystemSettings settings) {
        var client = sale.getClient();
        var property = sale.getProperty();
        String company = settings.getCompanyName() != null ? settings.getCompanyName() : settings.getSystemName();

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
                  <h1>CONTRATO DE COMPRA E VENDA DE IMÓVEL</h1>
                  <p>Por este instrumento particular, <strong>%s</strong> (VENDEDORA) e o(a) comprador(a)
                     abaixo qualificado(a) ajustam a compra e venda do imóvel descrito a seguir.</p>

                  <div class="section">
                    <strong>COMPRADOR(A)</strong><br/>
                    Nome/Razão Social: %s<br/>
                    CPF/CNPJ: %s &nbsp;&nbsp; RG/IE: %s<br/>
                    Endereço: %s<br/>
                    Estado civil: %s &nbsp;&nbsp; Profissão: %s<br/>
                    E-mail: %s &nbsp;&nbsp; Telefone: %s
                  </div>

                  <div class="section">
                    <strong>IMÓVEL</strong><br/>
                    Empreendimento: %s<br/>
                    Quadra: %s &nbsp; Lote: %s &nbsp; Unidade: %s<br/>
                    Matrícula: %s<br/>
                    Endereço: %s<br/>
                    Área total: %s m&#178; &nbsp; Área construída: %s m&#178;
                  </div>

                  <div class="section">
                    <strong>CONDIÇÕES DE PAGAMENTO</strong><br/>
                    Valor total: R$ %s<br/>
                    Entrada: R$ %s<br/>
                    Parcelas: %s &nbsp; Primeiro vencimento: %s<br/>
                    Forma de pagamento: %s &nbsp; Índice de correção: %s
                  </div>

                  <div class="section">
                    <strong>PARCELAS</strong>
                    <table>
                      <tr><th>Nº</th><th>Valor</th><th>Vencimento</th></tr>
                      %s
                    </table>
                  </div>

                  <p class="section">%s</p>

                  <p class="section">Local e data: ________________________, %s.</p>

                  <div class="signatures">
                    <span class="sig">VENDEDORA<br/>%s</span>
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    <span class="sig">COMPRADOR(A)<br/>%s</span>
                  </div>
                </body></html>
                """.formatted(
                company,
                nz(client.getName()), nz(client.getDocument()), nz(client.getStateRegistration()),
                nz(client.getAddress()), nz(client.getMaritalStatus()), nz(client.getOccupation()),
                nz(client.getEmail()), nz(client.getPhone()),
                nz(property.getDevelopment()), nz(property.getBlock()), nz(property.getLot()),
                nz(property.getUnit()), nz(property.getRegistration()), nz(property.getAddress()),
                nz(property.getTotalArea()), nz(property.getBuiltArea()),
                sale.getTotalValue(), sale.getDownPayment(), sale.getInstallmentsCount(),
                sale.getFirstDueDate().format(DATE), nz(sale.getPaymentMethod()), nz(sale.getCorrectionIndex()),
                installments.toString(),
                nz(property.getContractExtra()),
                LocalDate.now().format(DATE),
                company, nz(client.getName()));
    }

    private String nz(Object v) {
        return v == null ? "-" : v.toString();
    }
}
