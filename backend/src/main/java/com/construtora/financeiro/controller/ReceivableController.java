package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.receivable.ReceivableRejectRequest;
import com.construtora.financeiro.dto.receivable.ReceivableRequest;
import com.construtora.financeiro.dto.receivable.ReceivableResponse;
import com.construtora.financeiro.service.ReceivableService;
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
@RequestMapping("/api/accounts-receivable")
@Tag(name = "Accounts Receivable", description = "Contas a receber")
public class ReceivableController {

    private final ReceivableService service;

    public ReceivableController(ReceivableService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista/filtra contas a receber (paginado)")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_VIEW')")
    public Page<ReceivableResponse> list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) com.construtora.financeiro.model.enums.ReceivableStatus status,
                                         @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return service.search(q, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha conta a receber")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_VIEW')")
    public ReceivableResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria conta a receber")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_EDIT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceivableResponse create(@Valid @RequestBody ReceivableRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza conta a receber")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_EDIT')")
    public ReceivableResponse update(@PathVariable UUID id, @Valid @RequestBody ReceivableRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Confirma recebimento")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_EDIT')")
    public ReceivableResponse receive(@PathVariable UUID id,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate) {
        return service.confirmReceive(id, receiveDate);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprova conta a receber")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_EDIT')")
    public ReceivableResponse approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeita conta a receber")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_EDIT')")
    public ReceivableResponse reject(@PathVariable UUID id,
                                     @RequestBody(required = false) ReceivableRejectRequest request) {
        return service.reject(id, request != null ? request.reason() : null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove conta a receber")
    @PreAuthorize("hasAuthority('CONTAS_RECEBER_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
