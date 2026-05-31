package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.client.ClientRequest;
import com.construtora.financeiro.dto.client.ClientResponse;
import com.construtora.financeiro.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Cadastro de clientes")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista/pesquisa clientes (paginado)")
    @PreAuthorize("hasAuthority('READ')")
    public Page<ClientResponse> list(@RequestParam(required = false) String q,
                                     @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.search(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha cliente")
    @PreAuthorize("hasAuthority('READ')")
    public ClientResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria cliente")
    @PreAuthorize("hasAuthority('CLIENTS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza cliente")
    @PreAuthorize("hasAuthority('CLIENTS_WRITE')")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove cliente")
    @PreAuthorize("hasAuthority('CLIENTS_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
