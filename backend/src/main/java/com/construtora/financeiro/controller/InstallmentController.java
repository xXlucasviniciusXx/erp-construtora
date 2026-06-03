package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.sale.InstallmentDetailResponse;
import com.construtora.financeiro.dto.sale.InstallmentPaymentRequest;
import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import com.construtora.financeiro.service.InstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/installments")
@Tag(name = "Installments", description = "Parcelas de compra de imóveis")
public class InstallmentController {

    private final InstallmentService service;

    public InstallmentController(InstallmentService service) {
        this.service = service;
    }

    @GetMapping("/overdue")
    @Operation(summary = "Lista parcelas em atraso")
    @PreAuthorize("hasAuthority('READ')")
    public List<InstallmentResponse> overdue() {
        return service.findOverdue();
    }

    @GetMapping
    @Operation(summary = "Lista parcelas com dados do cliente e filtros (q, status, vencimento) — paginado")
    @PreAuthorize("hasAuthority('READ')")
    public Page<InstallmentDetailResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) InstallmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return service.search(q, status, dueFrom, dueTo, pageable);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Confirma pagamento de parcela")
    @PreAuthorize("hasAnyAuthority('RECEIVABLE_WRITE','SALES_WRITE')")
    public InstallmentResponse pay(@PathVariable UUID id, @Valid @RequestBody InstallmentPaymentRequest request) {
        return service.confirmPayment(id, request);
    }
}
