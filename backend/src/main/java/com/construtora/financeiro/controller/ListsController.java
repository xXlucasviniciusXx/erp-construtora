package com.construtora.financeiro.controller;

import com.construtora.financeiro.dto.lists.NamedItemRequest;
import com.construtora.financeiro.dto.lists.NamedItemResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.model.CorrectionIndex;
import com.construtora.financeiro.model.PaymentMethod;
import com.construtora.financeiro.model.SupplierCategory;
import com.construtora.financeiro.repository.CorrectionIndexRepository;
import com.construtora.financeiro.repository.PaymentMethodRepository;
import com.construtora.financeiro.repository.SupplierCategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gerencia as listas configuráveis de formas de pagamento e índices de correção.
 * Leitura: qualquer usuário autenticado.
 * Escrita: SETTINGS_MANAGE.
 */
@RestController
@RequestMapping("/api/lists")
@Tag(name = "Lists", description = "Listas configuráveis (formas de pagamento, índices de correção)")
public class ListsController {

    private final PaymentMethodRepository pmRepo;
    private final CorrectionIndexRepository ciRepo;
    private final SupplierCategoryRepository scRepo;

    public ListsController(PaymentMethodRepository pmRepo, CorrectionIndexRepository ciRepo,
                           SupplierCategoryRepository scRepo) {
        this.pmRepo = pmRepo;
        this.ciRepo = ciRepo;
        this.scRepo = scRepo;
    }

    // ---- Formas de pagamento ----

    @GetMapping("/payment-methods")
    @Operation(summary = "Lista formas de pagamento ativas")
    public List<NamedItemResponse> listPaymentMethods() {
        return pmRepo.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(p -> new NamedItemResponse(p.getId(), p.getName(), p.isActive(), p.getSortOrder()))
                .toList();
    }

    @GetMapping("/payment-methods/all")
    @Operation(summary = "Lista todas as formas de pagamento (admin)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public List<NamedItemResponse> listAllPaymentMethods() {
        return pmRepo.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(p -> new NamedItemResponse(p.getId(), p.getName(), p.isActive(), p.getSortOrder()))
                .toList();
    }

    @PostMapping("/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public NamedItemResponse createPaymentMethod(@Valid @RequestBody NamedItemRequest req) {
        if (pmRepo.existsByNameIgnoreCaseAndIdNot(req.name(), UUID.fromString("00000000-0000-0000-0000-000000000000")))
            throw new BusinessException("Já existe uma forma de pagamento com este nome.");
        PaymentMethod pm = new PaymentMethod();
        pm.setName(req.name().trim());
        pm.setActive(req.active());
        pm.setSortOrder(req.sortOrder());
        pm = pmRepo.save(pm);
        return new NamedItemResponse(pm.getId(), pm.getName(), pm.isActive(), pm.getSortOrder());
    }

    @PutMapping("/payment-methods/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public NamedItemResponse updatePaymentMethod(@PathVariable UUID id, @Valid @RequestBody NamedItemRequest req) {
        PaymentMethod pm = pmRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Forma de pagamento não encontrada."));
        if (pmRepo.existsByNameIgnoreCaseAndIdNot(req.name(), id))
            throw new BusinessException("Já existe uma forma de pagamento com este nome.");
        pm.setName(req.name().trim());
        pm.setActive(req.active());
        pm.setSortOrder(req.sortOrder());
        pm = pmRepo.save(pm);
        return new NamedItemResponse(pm.getId(), pm.getName(), pm.isActive(), pm.getSortOrder());
    }

    @DeleteMapping("/payment-methods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public void deletePaymentMethod(@PathVariable UUID id) {
        pmRepo.deleteById(id);
    }

    // ---- Índices de correção ----

    @GetMapping("/correction-indexes")
    @Operation(summary = "Lista índices de correção ativos")
    public List<NamedItemResponse> listCorrectionIndexes() {
        return ciRepo.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(c -> new NamedItemResponse(c.getId(), c.getName(), c.isActive(), c.getSortOrder()))
                .toList();
    }

    @GetMapping("/correction-indexes/all")
    @Operation(summary = "Lista todos os índices de correção (admin)")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public List<NamedItemResponse> listAllCorrectionIndexes() {
        return ciRepo.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(c -> new NamedItemResponse(c.getId(), c.getName(), c.isActive(), c.getSortOrder()))
                .toList();
    }

    @PostMapping("/correction-indexes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public NamedItemResponse createCorrectionIndex(@Valid @RequestBody NamedItemRequest req) {
        if (ciRepo.existsByNameIgnoreCaseAndIdNot(req.name(), UUID.fromString("00000000-0000-0000-0000-000000000000")))
            throw new BusinessException("Já existe um índice com este nome.");
        CorrectionIndex ci = new CorrectionIndex();
        ci.setName(req.name().trim());
        ci.setActive(req.active());
        ci.setSortOrder(req.sortOrder());
        ci = ciRepo.save(ci);
        return new NamedItemResponse(ci.getId(), ci.getName(), ci.isActive(), ci.getSortOrder());
    }

    @PutMapping("/correction-indexes/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public NamedItemResponse updateCorrectionIndex(@PathVariable UUID id, @Valid @RequestBody NamedItemRequest req) {
        CorrectionIndex ci = ciRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Índice de correção não encontrado."));
        if (ciRepo.existsByNameIgnoreCaseAndIdNot(req.name(), id))
            throw new BusinessException("Já existe um índice com este nome.");
        ci.setName(req.name().trim());
        ci.setActive(req.active());
        ci.setSortOrder(req.sortOrder());
        ci = ciRepo.save(ci);
        return new NamedItemResponse(ci.getId(), ci.getName(), ci.isActive(), ci.getSortOrder());
    }

    @DeleteMapping("/correction-indexes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public void deleteCorrectionIndex(@PathVariable UUID id) {
        ciRepo.deleteById(id);
    }

    // ---- Categorias de fornecedor ----

    @GetMapping("/supplier-categories")
    @Operation(summary = "Lista categorias de fornecedor ativas")
    public List<NamedItemResponse> listSupplierCategories() {
        return scRepo.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(c -> new NamedItemResponse(c.getId(), c.getName(), c.isActive(), c.getSortOrder()))
                .toList();
    }

    @GetMapping("/supplier-categories/all")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public List<NamedItemResponse> listAllSupplierCategories() {
        return scRepo.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(c -> new NamedItemResponse(c.getId(), c.getName(), c.isActive(), c.getSortOrder()))
                .toList();
    }

    @PostMapping("/supplier-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public NamedItemResponse createSupplierCategory(@Valid @RequestBody NamedItemRequest req) {
        if (scRepo.existsByNameIgnoreCaseAndIdNot(req.name(), UUID.fromString("00000000-0000-0000-0000-000000000000")))
            throw new BusinessException("Já existe uma categoria com este nome.");
        SupplierCategory sc = new SupplierCategory();
        sc.setName(req.name().trim()); sc.setActive(req.active()); sc.setSortOrder(req.sortOrder());
        sc = scRepo.save(sc);
        return new NamedItemResponse(sc.getId(), sc.getName(), sc.isActive(), sc.getSortOrder());
    }

    @PutMapping("/supplier-categories/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public NamedItemResponse updateSupplierCategory(@PathVariable UUID id, @Valid @RequestBody NamedItemRequest req) {
        SupplierCategory sc = scRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Categoria não encontrada."));
        if (scRepo.existsByNameIgnoreCaseAndIdNot(req.name(), id))
            throw new BusinessException("Já existe uma categoria com este nome.");
        sc.setName(req.name().trim()); sc.setActive(req.active()); sc.setSortOrder(req.sortOrder());
        sc = scRepo.save(sc);
        return new NamedItemResponse(sc.getId(), sc.getName(), sc.isActive(), sc.getSortOrder());
    }

    @DeleteMapping("/supplier-categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public void deleteSupplierCategory(@PathVariable UUID id) {
        scRepo.deleteById(id);
    }
}
