package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.dto.sale.SaleRequest;
import com.construtora.financeiro.dto.sale.SaleResponse;
import com.construtora.financeiro.service.InstallmentService;
import com.construtora.financeiro.service.SaleService;
import com.construtora.financeiro.model.enums.SaleStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    @Operation(summary = "Lista/filtra vendas (paginado): cliente, status, texto")
    @PreAuthorize("hasAuthority('READ')")
    public Page<SaleResponse> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) SaleStatus status,
                                   @RequestParam(required = false) UUID clientId,
                                   @PageableDefault(size = 20, sort = "saleDate") Pageable pageable) {
        return saleService.search(q, status, clientId, pageable);
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

    @PutMapping("/{id}")
    @Operation(summary = "Edita venda (regenera parcelas se valor/qtd mudar e nenhuma estiver paga)")
    @PreAuthorize("hasAuthority('SALES_WRITE')")
    public SaleResponse update(@PathVariable UUID id, @Valid @RequestBody SaleRequest request) {
        return saleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela/remove venda (libera o imóvel)")
    @PreAuthorize("hasAuthority('SALES_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        saleService.delete(id);
    }
}
