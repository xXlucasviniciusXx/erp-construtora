package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.development.DevelopmentRequest;
import com.construtora.financeiro.dto.development.DevelopmentResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.repository.BlockRepository;
import com.construtora.financeiro.repository.DevelopmentRepository;
import com.construtora.financeiro.repository.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DevelopmentService {

    private final DevelopmentRepository repository;
    private final BlockRepository blockRepository;
    private final LotRepository lotRepository;

    public DevelopmentService(DevelopmentRepository repository, BlockRepository blockRepository, LotRepository lotRepository) {
        this.repository = repository;
        this.blockRepository = blockRepository;
        this.lotRepository = lotRepository;
    }

    @Transactional(readOnly = true)
    public List<DevelopmentResponse> findAll() {
        return repository.findAllByOrderByName().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DevelopmentResponse findById(UUID id) {
        return toResponse(getEntity(id));
    }

    public DevelopmentResponse create(DevelopmentRequest r) {
        Development d = new Development();
        d.setInternalCode(nextCode());
        apply(r, d);
        return toResponse(repository.save(d));
    }

    public DevelopmentResponse update(UUID id, DevelopmentRequest r) {
        Development d = getEntity(id);
        apply(r, d);
        return toResponse(repository.save(d));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public Development getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Empreendimento", id));
    }

    private void apply(DevelopmentRequest r, Development d) {
        d.setName(r.name());
        d.setBlocksCount(r.blocksCount());
        d.setLotsCount(r.lotsCount());
        d.setExpectedValue(r.expectedValue() != null ? r.expectedValue() : BigDecimal.ZERO);
        d.setAddress(r.address());
        d.setStatus(r.status() != null ? r.status() : "ACTIVE");
        d.setDimensions(r.dimensions());

        String category = "TERRENISTA".equals(r.category()) ? "TERRENISTA" : "CORRETORA";
        d.setCategory(category);
        // Campos de terrenista só fazem sentido na categoria Terrenista.
        if ("TERRENISTA".equals(category)) {
            d.setTerrenistaCount(r.terrenistaCount());
            d.setDivisionPercent(r.divisionPercent());
        } else {
            d.setTerrenistaCount(null);
            d.setDivisionPercent(null);
        }
    }

    /** Código sequencial E001, E002, ... */
    private String nextCode() {
        long next = repository.count() + 1;
        return String.format("E%03d", next);
    }

    private DevelopmentResponse toResponse(Development d) {
        return new DevelopmentResponse(
                d.getId(), d.getName(), d.getInternalCode(), d.getBlocksCount(), d.getLotsCount(),
                d.getExpectedValue(),
                lotRepository.plannedTotalByDevelopment(d.getId()),
                lotRepository.receivedTotalByDevelopment(d.getId()),
                blockRepository.countByDevelopmentId(d.getId()),
                lotRepository.countByBlockDevelopmentId(d.getId()),
                d.getAddress(), d.getStatus(), d.getDimensions(),
                d.getCategory(), d.getTerrenistaCount(), d.getDivisionPercent());
    }
}
