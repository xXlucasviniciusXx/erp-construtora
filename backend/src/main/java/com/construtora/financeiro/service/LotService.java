package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.lot.LotRequest;
import com.construtora.financeiro.dto.lot.LotResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.LotMapper;
import com.construtora.financeiro.model.Block;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.Lot;
import com.construtora.financeiro.model.enums.PropertyStatus;
import com.construtora.financeiro.repository.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LotService {

    private final LotRepository repository;
    private final BlockService blockService;
    private final LotMapper mapper;

    public LotService(LotRepository repository, BlockService blockService, LotMapper mapper) {
        this.repository = repository;
        this.blockService = blockService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<LotResponse> findByBlock(UUID blockId) {
        return repository.findByBlockIdOrderByInternalCode(blockId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LotResponse> findByDevelopment(UUID developmentId) {
        return repository.findByBlockDevelopmentIdOrderByInternalCode(developmentId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LotResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    public LotResponse create(LotRequest r) {
        Block block = blockService.getEntity(r.blockId());
        Development dev = block.getDevelopment();
        long lotsInDev = repository.countByBlockDevelopmentId(dev.getId());
        if (dev.getLotsCount() != null && lotsInDev >= dev.getLotsCount()) {
            throw new BusinessException("Limite de lotes do empreendimento atingido (" + dev.getLotsCount() + ")");
        }
        Lot lot = new Lot();
        lot.setBlock(block);
        long inBlock = repository.countByBlockId(block.getId());
        lot.setInternalCode(block.getInternalCode() + String.format("-L%03d", inBlock + 1));
        apply(r, lot);
        return mapper.toResponse(repository.save(lot));
    }

    public LotResponse update(UUID id, LotRequest r) {
        Lot lot = getEntity(id);
        apply(r, lot);   // saleValue e internalCode não são alterados aqui
        return mapper.toResponse(repository.save(lot));
    }

    public LotResponse cancel(UUID id) {
        Lot lot = getEntity(id);
        lot.setStatus(PropertyStatus.CANCELLED);
        return mapper.toResponse(repository.save(lot));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public Lot getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lote", id));
    }

    private void apply(LotRequest r, Lot lot) {
        lot.setName(r.name());
        lot.setRegistration(r.registration());
        lot.setUnit(r.unit());
        lot.setAddress(r.address());
        lot.setTotalArea(r.totalArea());
        lot.setBuiltArea(r.builtArea());
        lot.setPlannedValue(r.plannedValue());
        lot.setContractExtra(r.contractExtra());
        lot.setNotes(r.notes());
        if (r.status() != null) {
            lot.setStatus(r.status());
        } else if (lot.getStatus() == null) {
            lot.setStatus(PropertyStatus.AVAILABLE);
        }
    }
}
