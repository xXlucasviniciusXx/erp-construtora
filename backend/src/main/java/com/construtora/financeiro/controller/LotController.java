package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.lot.LotRequest;
import com.construtora.financeiro.dto.lot.LotResponse;
import com.construtora.financeiro.service.LotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lots")
@Tag(name = "Lots", description = "Lotes (nível 3 da hierarquia)")
public class LotController {

    private final LotService service;

    public LotController(LotService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista lotes (por quadra, por empreendimento ou todos)")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_VIEW')")
    public List<LotResponse> list(@RequestParam(required = false) UUID blockId,
                                  @RequestParam(required = false) UUID developmentId) {
        if (blockId != null) return service.findByBlock(blockId);
        if (developmentId != null) return service.findByDevelopment(developmentId);
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Cria lote (respeita o limite de lotes do empreendimento)")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse create(@Valid @RequestBody LotRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza lote")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    public LotResponse update(@PathVariable UUID id, @Valid @RequestBody LotRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Inativa/cancela lote")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    public LotResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove lote")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
