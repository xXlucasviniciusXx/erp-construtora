package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.category.CategoryRequest;
import com.construtora.financeiro.dto.category.CategoryResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.Category;
import com.construtora.financeiro.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAllByOrderByGrupoAscNameAsc().stream().map(this::toResponse).toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        return toResponse(repository.save(apply(request, new Category())));
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        return toResponse(repository.save(apply(request, getEntity(id))));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Category getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", id));
    }

    private Category apply(CategoryRequest r, Category c) {
        c.setGrupo(r.grupo().trim());
        c.setName(r.name().trim());
        c.setActive(r.active() == null || r.active());
        return c;
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getGrupo(), c.getName(), c.isActive());
    }
}
