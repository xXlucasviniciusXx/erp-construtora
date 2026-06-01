package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.dto.sale.SaleRequest;
import com.construtora.financeiro.dto.sale.SaleResponse;
import com.construtora.financeiro.service.InstallmentService;
import com.construtora.financeiro.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales", description = "Vendas de imóveis (gera parcelas automaticamente)")
public class SaleController {

    private final SaleService saleService;
    private final InstallmentService installmentService;

    public SaleController(SaleService saleService, InstallmentService installmentService) {
        this.saleService = saleService;
        this.installmentService = installmentService;
    }

    @GetMapping
    @Operation(summary = "Lista vendas (opcionalmente por cliente)")
    @PreAuthorize("hasAuthority('READ')")
    public List<SaleResponse> list(@RequestParam(required = false) UUID clientId) {
        return clientId != null ? saleService.findByClient(clientId) : saleService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha venda com parcelas")
    @PreAuthorize("hasAuthority('READ')")
    public SaleResponse get(@PathVariable UUID id) {
        return saleService.findById(id);
    }

    @GetMapping("/{id}/installments")
    @Operation(summary = "Lista parcelas da venda")
    @PreAuthorize("hasAuthority('READ')")
    public List<InstallmentResponse> installments(@PathVariable UUID id) {
        return installmentService.findBySale(id);
    }

    @PostMapping
    @Operation(summary = "Registra venda e gera parcelas")
    @PreAuthorize("hasAuthority('SALES_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@Valid @RequestBody SaleRequest request) {
        return saleService.create(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela/remove venda (libera o imóvel)")
    @PreAuthorize("hasAuthority('SALES_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        saleService.delete(id);
    }
}
