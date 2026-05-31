package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.sale.InstallmentPaymentRequest;
import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.service.InstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/pay")
    @Operation(summary = "Confirma pagamento de parcela")
    @PreAuthorize("hasAnyAuthority('RECEIVABLE_WRITE','SALES_WRITE')")
    public InstallmentResponse pay(@PathVariable UUID id, @Valid @RequestBody InstallmentPaymentRequest request) {
        return service.confirmPayment(id, request);
    }
}
