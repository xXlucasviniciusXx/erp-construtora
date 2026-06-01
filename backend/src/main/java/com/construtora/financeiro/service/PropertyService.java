package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.property.PropertyRequest;
import com.construtora.financeiro.dto.property.PropertyResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.PropertyMapper;
import com.construtora.financeiro.model.Property;
import com.construtora.financeiro.repository.PropertyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PropertyService {

    private final PropertyRepository repository;
    private final PropertyMapper mapper;

    public PropertyService(PropertyRepository repository, PropertyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PropertyResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PropertyResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    public PropertyResponse create(PropertyRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request, null)));
    }

    public PropertyResponse update(UUID id, PropertyRequest request) {
        Property property = getEntity(id);
        return mapper.toResponse(repository.save(mapper.toEntity(request, property)));
    }

    public PropertyResponse cancel(UUID id) {
        Property property = getEntity(id);
        property.setStatus(com.construtora.financeiro.model.enums.PropertyStatus.CANCELLED);
        return mapper.toResponse(repository.save(property));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public Property getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Imóvel", id));
    }
}
