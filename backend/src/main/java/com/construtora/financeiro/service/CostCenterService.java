package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.costcenter.CostCenterRequest;
import com.construtora.financeiro.dto.costcenter.CostCenterResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.CostCenter;
import com.construtora.financeiro.repository.CostCenterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CostCenterService {

    private final CostCenterRepository repository;

    public CostCenterService(CostCenterRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CostCenterResponse> findAll() {
        return repository.findAllByOrderByName().stream().map(this::toResponse).toList();
    }

    public CostCenterResponse create(CostCenterRequest request) {
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessException("Já existe um centro de custo com esse nome");
        }
        return toResponse(repository.save(apply(request, new CostCenter())));
    }

    public CostCenterResponse update(UUID id, CostCenterRequest request) {
        return toResponse(repository.save(apply(request, getEntity(id))));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private CostCenter getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Centro de custo", id));
    }

    private CostCenter apply(CostCenterRequest r, CostCenter c) {
        c.setName(r.name().trim());
        c.setDescription(r.description());
        c.setActive(r.active() == null || r.active());
        return c;
    }

    private CostCenterResponse toResponse(CostCenter c) {
        return new CostCenterResponse(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }
}
