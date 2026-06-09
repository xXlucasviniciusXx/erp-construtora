package com.construtora.financeiro.service.contract;

import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.ContractDocument;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.SaleStatus;
import com.construtora.financeiro.repository.ContractDocumentRepository;
import com.construtora.financeiro.security.SecurityUtils;
import com.construtora.financeiro.service.NotificationService;
import com.construtora.financeiro.service.SaleService;
import com.construtora.financeiro.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private final SaleService saleService;
    private final SettingsService settingsService;
    private final ContractTemplateService templateService;
    private final ContractRenderer renderer;
    private final ContractDocumentRepository documentRepository;
    private final NotificationService notificationService;
    private final com.construtora.financeiro.security.DevelopmentScopeService scope;

    public ContractService(SaleService saleService, SettingsService settingsService,
                           ContractTemplateService templateService, ContractRenderer renderer,
                           ContractDocumentRepository documentRepository,
                           NotificationService notificationService,
                           com.construtora.financeiro.security.DevelopmentScopeService scope) {
        this.saleService = saleService;
        this.settingsService = settingsService;
        this.templateService = templateService;
        this.renderer = renderer;
        this.documentRepository = documentRepository;
        this.notificationService = notificationService;
        this.scope = scope;
    }

    /** Garante que o usuário tem escopo sobre o empreendimento da venda (anti-IDOR). */
    private void checkScope(PropertySale sale, Object id) {
        scope.requireAccess(sale.getLot().getBlock().getDevelopment().getId(), "Venda", id);
    }

    // ---- HTML (pré-visualização) ----

    @Transactional(readOnly = true)
    public String generateHtml(UUID saleId) {
        PropertySale sale = saleService.getEntity(saleId);
        checkScope(sale, saleId);
        return buildDocument(sale, "CONTRACT");
    }

    /**
     * Monta o XHTML final do documento: extrai o fragmento do modelo padrão,
     * substitui os tokens com os dados da venda e embrulha no esqueleto + CSS,
     * normalizando para XHTML bem-formado (exigido pelo gerador de PDF).
     */
    private String buildDocument(PropertySale sale, String kind) {
        UUID developmentId = sale.getLot().getBlock().getDevelopment().getId();
        String fragment = ContractHtml.extractFragment(templateService.defaultBodyFor(kind, developmentId));
        String substituted = renderer.render(sale, settingsService.current(), fragment);
        return ContractHtml.document(substituted);
    }

    // ---- Geração de PDF + arquivamento ----

    public byte[] generateContractPdf(UUID saleId) {
        PropertySale sale = saleService.getEntity(saleId);
        checkScope(sale, saleId);
        byte[] pdf = toPdf(buildDocument(sale, "CONTRACT"), "contrato");
        archive(sale, "CONTRACT", pdf);
        notificationService.notifyContractGenerated(sale);
        return pdf;
    }

    public byte[] generateDistratoPdf(UUID saleId) {
        PropertySale sale = saleService.getEntity(saleId);
        checkScope(sale, saleId);
        if (sale.getStatus() != SaleStatus.CANCELLED || sale.getDistratoDate() == null) {
            throw new BusinessException("Distrato disponível apenas para vendas já distratadas.");
        }
        byte[] pdf = toPdf(buildDocument(sale, "DISTRATO"), "distrato");
        archive(sale, "DISTRATO", pdf);
        return pdf;
    }

    private byte[] toPdf(String html, String label) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Falha ao gerar PDF do " + label
                    + ". Verifique se o modelo (Configurações → Contratos) é um XHTML válido. Detalhe: " + e.getMessage());
        }
    }

    /** Salva uma cópia versionada do documento gerado. */
    private void archive(PropertySale sale, String type, byte[] pdf) {
        int version = documentRepository.countBySaleIdAndType(sale.getId(), type) + 1;
        ContractDocument doc = new ContractDocument();
        doc.setSale(sale);
        doc.setType(type);
        doc.setVersion(version);
        String number = sale.getContractNumber() != null ? sale.getContractNumber() : sale.getId().toString();
        doc.setFileName((type.equals("DISTRATO") ? "distrato-" : "contrato-") + number + "-v" + version + ".pdf");
        doc.setPdfData(pdf);
        SecurityUtils.currentUsername().ifPresent(doc::setGeneratedBy);
        documentRepository.save(doc);
    }

    // ---- Arquivo de documentos ----

    @Transactional(readOnly = true)
    public List<ContractDocument> listDocuments(UUID saleId) {
        checkScope(saleService.getEntity(saleId), saleId);
        return documentRepository.findBySaleIdOrderByGeneratedAtDesc(saleId);
    }

    @Transactional(readOnly = true)
    public ContractDocument getDocument(UUID documentId) {
        ContractDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Documento", documentId));
        checkScope(doc.getSale(), documentId);
        return doc;
    }
}
