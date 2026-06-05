package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.contract.ContractTemplateRequest;
import com.construtora.financeiro.dto.contract.ContractTemplateResponse;
import com.construtora.financeiro.dto.contract.TemplatePreviewRequest;
import com.construtora.financeiro.service.contract.ContractTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contract-templates")
@Tag(name = "Contract Templates", description = "Modelos de contrato/distrato editáveis")
public class ContractTemplateController {

    private final ContractTemplateService service;

    public ContractTemplateController(ContractTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista modelos (opcionalmente por tipo CONTRACT/DISTRATO)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public List<ContractTemplateResponse> list(@RequestParam(required = false) String kind) {
        return service.list(kind).stream().map(ContractTemplateResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um modelo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ContractTemplateResponse get(@PathVariable UUID id) {
        return ContractTemplateResponse.from(service.get(id));
    }

    @PostMapping
    @Operation(summary = "Cria um modelo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractTemplateResponse create(@Valid @RequestBody ContractTemplateRequest request) {
        return ContractTemplateResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita um modelo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ContractTemplateResponse update(@PathVariable UUID id, @Valid @RequestBody ContractTemplateRequest request) {
        return ContractTemplateResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um modelo (exceto o padrão)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Pré-visualiza um corpo de modelo com dados de exemplo")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<String> preview(@Valid @RequestBody TemplatePreviewRequest request) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(service.previewSample(request.body()));
    }
}
