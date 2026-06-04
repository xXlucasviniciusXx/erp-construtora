package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.lot.LotRequest;
import com.construtora.financeiro.dto.lot.LotReserveRequest;
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
    @Operation(summary = "Lista lotes. Suporta ?blockId=, ?developmentId= ou ?q= (busca textual)")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_VIEW')")
    public List<LotResponse> list(@RequestParam(required = false) UUID blockId,
                                  @RequestParam(required = false) UUID developmentId,
                                  @RequestParam(required = false) String q) {
        if (blockId != null) return service.findByBlock(blockId);
        if (developmentId != null) return service.findByDevelopment(developmentId);
        if (q != null) return service.search(q);   // busca textual (combobox server-side)
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna um lote pelo ID (usado para pré-carregar o combobox)")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_VIEW')")
    public LotResponse findById(@PathVariable UUID id) {
        return service.findById(id);
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

    @PatchMapping("/{id}/reserve")
    @Operation(summary = "Reserva lote AVAILABLE por N horas (padrão: 24 h)")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    public LotResponse reserve(@PathVariable UUID id, @RequestBody(required = false) LotReserveRequest req) {
        return service.reserve(id, req != null ? req : new LotReserveRequest(null));
    }

    @PatchMapping("/{id}/release")
    @Operation(summary = "Libera reserva manualmente, devolvendo lote a AVAILABLE")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    public LotResponse release(@PathVariable UUID id) {
        return service.release(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove lote")
    @PreAuthorize("hasAuthority('EMPREENDIMENTOS_EDIT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
