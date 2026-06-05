package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.contract.ContractDocumentResponse;
import com.construtora.financeiro.model.ContractDocument;
import com.construtora.financeiro.service.contract.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contracts", description = "Geração de contratos/distratos (HTML/PDF) e arquivo de documentos")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping(value = "/sales/{saleId}/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Gera contrato em HTML (pré-visualização)")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public ResponseEntity<String> html(@PathVariable UUID saleId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(service.generateHtml(saleId));
    }

    @GetMapping(value = "/sales/{saleId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Gera contrato em PDF (arquiva a versão)")
    @PreAuthorize("hasAuthority('VENDAS_EDIT')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID saleId) {
        byte[] pdf = service.generateContractPdf(saleId);
        return pdfResponse(pdf, "contrato-" + saleId + ".pdf");
    }

    @GetMapping(value = "/sales/{saleId}/distrato/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Gera o distrato em PDF (somente vendas distratadas)")
    @PreAuthorize("hasAuthority('VENDAS_EDIT')")
    public ResponseEntity<byte[]> distratoPdf(@PathVariable UUID saleId) {
        byte[] pdf = service.generateDistratoPdf(saleId);
        return pdfResponse(pdf, "distrato-" + saleId + ".pdf");
    }

    @GetMapping("/sales/{saleId}/documents")
    @Operation(summary = "Lista os documentos arquivados da venda")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public List<ContractDocumentResponse> documents(@PathVariable UUID saleId) {
        return service.listDocuments(saleId).stream().map(ContractDocumentResponse::from).toList();
    }

    @GetMapping(value = "/documents/{documentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Baixa um documento arquivado")
    @PreAuthorize("hasAuthority('VENDAS_VIEW')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID documentId) {
        ContractDocument doc = service.getDocument(documentId);
        return pdfResponse(doc.getPdfData(), doc.getFileName());
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
