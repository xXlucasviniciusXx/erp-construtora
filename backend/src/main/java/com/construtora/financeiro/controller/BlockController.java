package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.block.BlockRequest;
import com.construtora.financeiro.dto.block.BlockResponse;
import com.construtora.financeiro.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blocks")
@Tag(name = "Blocks", description = "Quadras (nível 2 da hierarquia)")
public class BlockController {

    private final BlockService service;

    public BlockController(BlockService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista quadras de um empreendimento")
    @PreAuthorize("hasAuthority('READ')")
    public List<BlockResponse> list(@RequestParam UUID developmentId) {
        return service.findByDevelopment(developmentId);
    }

    @PostMapping
    @Operation(summary = "Cria quadra (respeita o limite do empreendimento)")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public BlockResponse create(@Valid @RequestBody BlockRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza quadra (nome, matrícula, área)")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    public BlockResponse update(@PathVariable UUID id, @Valid @RequestBody BlockRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove quadra")
    @PreAuthorize("hasAuthority('PROPERTIES_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
