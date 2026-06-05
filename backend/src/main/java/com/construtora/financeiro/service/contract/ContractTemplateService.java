package com.construtora.financeiro.service.contract;

import com.construtora.financeiro.dto.contract.ContractTemplateRequest;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.ContractTemplate;
import com.construtora.financeiro.repository.ContractTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CRUD dos modelos de contrato/distrato editáveis pelo ADMIN, além de fornecer
 * o corpo do template padrão usado na geração de documentos.
 */
@Service
@Transactional
public class ContractTemplateService {

    private static final Set<String> KINDS = Set.of("CONTRACT", "DISTRATO");

    private final ContractTemplateRepository repository;

    public ContractTemplateService(ContractTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ContractTemplate> list(String kind) {
        return (kind != null && !kind.isBlank())
                ? repository.findByKindOrderByNameAsc(kind.toUpperCase())
                : repository.findAll();
    }

    @Transactional(readOnly = true)
    public ContractTemplate get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Modelo de contrato", id));
    }

    /** Corpo do template padrão do tipo informado (CONTRACT/DISTRATO). */
    @Transactional(readOnly = true)
    public String defaultBody(String kind) {
        return repository.findFirstByKindAndIsDefaultTrue(kind)
                .or(() -> repository.findByKindOrderByNameAsc(kind).stream().findFirst())
                .map(ContractTemplate::getBody)
                .orElseThrow(() -> new BusinessException(
                        "Nenhum modelo de " + (kind.equals("DISTRATO") ? "distrato" : "contrato") + " configurado."));
    }

    public ContractTemplate create(ContractTemplateRequest r) {
        ContractTemplate t = new ContractTemplate();
        applyBasics(t, r);
        // Zera o padrão dos demais ANTES de marcar este (t ainda não está no banco).
        if (Boolean.TRUE.equals(r.isDefault())) {
            repository.clearDefaults(t.getKind(), null);
            t.setDefault(true);
        }
        return repository.save(t);
    }

    public ContractTemplate update(UUID id, ContractTemplateRequest r) {
        ContractTemplate t = get(id);
        applyBasics(t, r);
        if (Boolean.TRUE.equals(r.isDefault())) {
            repository.clearDefaults(t.getKind(), id);   // zera os outros; mantém t
            t.setDefault(true);
        } else {
            t.setDefault(false);
        }
        return repository.save(t);
    }

    public void delete(UUID id) {
        ContractTemplate t = get(id);
        if (t.isDefault()) {
            throw new BusinessException("Não é possível remover o modelo padrão. Defina outro como padrão antes.");
        }
        repository.delete(t);
    }

    /** Aplica os campos básicos (sem mexer no flag de padrão, tratado à parte). */
    private void applyBasics(ContractTemplate t, ContractTemplateRequest r) {
        String kind = r.kind() != null ? r.kind().toUpperCase() : "CONTRACT";
        if (!KINDS.contains(kind)) {
            throw new BusinessException("Tipo de modelo inválido: " + r.kind());
        }
        t.setKind(kind);
        t.setName(r.name());
        // Sanitiza o fragmento (remove script/handlers, preserva formatação e tokens).
        t.setBody(ContractHtml.sanitizeFragment(r.body()));
        t.setActive(r.active() == null || r.active());
    }

    // ---- Preview com dados de exemplo (não exige uma venda real) ----

    private static final java.util.Map<String, String> SAMPLE = java.util.Map.ofEntries(
            java.util.Map.entry("empresa", "Construtora Exemplo Ltda."),
            java.util.Map.entry("numero_contrato", "CT-000123"),
            java.util.Map.entry("cliente_nome", "João da Silva"),
            java.util.Map.entry("cliente_documento", "123.456.789-00"),
            java.util.Map.entry("cliente_rg_ie", "MG-12.345.678"),
            java.util.Map.entry("cliente_endereco", "Rua das Flores, 100 - Centro"),
            java.util.Map.entry("cliente_estado_civil", "Casado(a)"),
            java.util.Map.entry("cliente_profissao", "Engenheiro"),
            java.util.Map.entry("cliente_email", "joao@example.com"),
            java.util.Map.entry("cliente_telefone", "(31) 99999-0000"),
            java.util.Map.entry("empreendimento", "Residencial Jardim"),
            java.util.Map.entry("quadra", "A"),
            java.util.Map.entry("lote", "12"),
            java.util.Map.entry("unidade", "—"),
            java.util.Map.entry("matricula", "98.765"),
            java.util.Map.entry("imovel_endereco", "Quadra A, Lote 12 - Residencial Jardim"),
            java.util.Map.entry("area_total", "250"),
            java.util.Map.entry("area_construida", "0"),
            java.util.Map.entry("valor_total", "180000.00"),
            java.util.Map.entry("entrada", "30000.00"),
            java.util.Map.entry("parcelas_qtd", "120"),
            java.util.Map.entry("primeiro_vencimento", "10/01/2026"),
            java.util.Map.entry("forma_pagamento", "Boleto"),
            java.util.Map.entry("indice_correcao", "INCC"),
            java.util.Map.entry("clausulas_extras", "Cláusulas adicionais específicas do lote aparecem aqui."),
            java.util.Map.entry("data_hoje", "05/06/2026"),
            java.util.Map.entry("distrato_data", "05/06/2026"),
            java.util.Map.entry("distrato_motivo", "Rescisão por desistência do comprador."),
            java.util.Map.entry("distrato_devolucao", "24000.00"),
            java.util.Map.entry("distrato_retido", "6000.00"),
            java.util.Map.entry("parcelas_tabela",
                    "<table class=\"parcelas\"><tr><th>Nº</th><th>Valor</th><th>Vencimento</th></tr>"
                    + "<tr><td>1</td><td>R$ 1250.00</td><td>10/01/2026</td></tr>"
                    + "<tr><td>2</td><td>R$ 1250.00</td><td>10/02/2026</td></tr></table>"));

    /**
     * Renderiza o corpo recebido com dados de exemplo e embrulha no esqueleto +
     * CSS, devolvendo o XHTML completo (para o iframe de pré-visualização).
     */
    @Transactional(readOnly = true)
    public String previewSample(String body) {
        String fragment = ContractHtml.extractFragment(body);
        Matcher m = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}").matcher(fragment);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String v = SAMPLE.getOrDefault(m.group(1), "");
            m.appendReplacement(out, Matcher.quoteReplacement(v));
        }
        m.appendTail(out);
        return ContractHtml.document(out.toString());
    }
}
