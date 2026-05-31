package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.payable.PayableRequest;
import com.construtora.financeiro.dto.payable.PayableResponse;
import com.construtora.financeiro.service.PayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts-payable")
@Tag(name = "Accounts Payable", description = "Contas a pagar")
public class PayableController {

    private final PayableService service;

    public PayableController(PayableService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista contas a pagar")
    @PreAuthorize("hasAuthority('READ')")
    public Page<PayableResponse> list(@PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha conta a pagar")
    @PreAuthorize("hasAuthority('READ')")
    public PayableResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria conta a pagar")
    @PreAuthorize("hasAuthority('PAYABLE_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PayableResponse create(@Valid @RequestBody PayableRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza conta a pagar")
    @PreAuthorize("hasAuthority('PAYABLE_WRITE')")
    public PayableResponse update(@PathVariable UUID id, @Valid @RequestBody PayableRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Confirma pagamento")
    @PreAuthorize("hasAuthority('PAYABLE_WRITE')")
    public PayableResponse pay(@PathVariable UUID id,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate) {
        return service.confirmPayment(id, paymentDate);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove conta a pagar")
    @PreAuthorize("hasAuthority('PAYABLE_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
