package com.construtora.financeiro.service.contract;

import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.service.NotificationService;
import com.construtora.financeiro.service.SaleService;
import com.construtora.financeiro.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ContractService {

    private final SaleService saleService;
    private final SettingsService settingsService;
    private final ContractTemplateService templateService;
    private final NotificationService notificationService;

    public ContractService(SaleService saleService, SettingsService settingsService,
                           ContractTemplateService templateService, NotificationService notificationService) {
        this.saleService = saleService;
        this.settingsService = settingsService;
        this.templateService = templateService;
        this.notificationService = notificationService;
    }

    public String generateHtml(UUID saleId) {
        PropertySale sale = saleService.getEntity(saleId);
        return templateService.render(sale, settingsService.current());
    }

    public byte[] generatePdf(UUID saleId) {
        PropertySale sale = saleService.getEntity(saleId);
        String html = templateService.render(sale, settingsService.current());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            notificationService.notifyContractGenerated(sale);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF do contrato: " + e.getMessage(), e);
        }
    }
}
