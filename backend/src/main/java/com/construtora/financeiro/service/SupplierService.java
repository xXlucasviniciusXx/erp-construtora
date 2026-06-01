package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.supplier.SupplierRequest;
import com.construtora.financeiro.dto.supplier.SupplierResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.Supplier;
import com.construtora.financeiro.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll(String q) {
        List<Supplier> list = (q == null || q.isBlank())
                ? repository.findAllByOrderByName()
                : repository.findByNameContainingIgnoreCaseOrderByName(q.trim());
        return list.stream().map(this::toResponse).toList();
    }

    public SupplierResponse create(SupplierRequest request) {
        return toResponse(repository.save(apply(request, new Supplier())));
    }

    public SupplierResponse update(UUID id, SupplierRequest request) {
        return toResponse(repository.save(apply(request, getEntity(id))));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Supplier getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Fornecedor", id));
    }

    private Supplier apply(SupplierRequest r, Supplier s) {
        s.setName(r.name());
        s.setDocument(r.document() == null ? null : r.document().replaceAll("\\D", ""));
        s.setEmail(r.email());
        s.setPhone(r.phone());
        s.setAddress(r.address());
        s.setCity(r.city());
        s.setState(r.state());
        s.setCategory(r.category());
        s.setNotes(r.notes());
        s.setActive(r.active() == null || r.active());
        return s;
    }

    private SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getDocument(), s.getEmail(), s.getPhone(),
                s.getAddress(), s.getCity(), s.getState(), s.getCategory(), s.getNotes(), s.isActive());
    }
}
