package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.block.BlockRequest;
import com.construtora.financeiro.dto.block.BlockResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.Block;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.repository.BlockRepository;
import com.construtora.financeiro.repository.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BlockService {

    private final BlockRepository repository;
    private final LotRepository lotRepository;
    private final DevelopmentService developmentService;

    public BlockService(BlockRepository repository, LotRepository lotRepository, DevelopmentService developmentService) {
        this.repository = repository;
        this.lotRepository = lotRepository;
        this.developmentService = developmentService;
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> findByDevelopment(UUID developmentId) {
        return repository.findByDevelopmentIdOrderByInternalCode(developmentId).stream().map(this::toResponse).toList();
    }

    public BlockResponse create(BlockRequest r) {
        Development dev = developmentService.getEntity(r.developmentId());
        long current = repository.countByDevelopmentId(dev.getId());
        if (dev.getBlocksCount() != null && current >= dev.getBlocksCount()) {
            throw new BusinessException("Limite de quadras do empreendimento atingido (" + dev.getBlocksCount() + ")");
        }
        Block b = new Block();
        b.setDevelopment(dev);
        b.setInternalCode(dev.getInternalCode() + String.format("-Q%02d", current + 1));
        apply(r, b);
        return toResponse(repository.save(b));
    }

    public BlockResponse update(UUID id, BlockRequest r) {
        Block b = getEntity(id);
        apply(r, b);
        return toResponse(repository.save(b));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public Block getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Quadra", id));
    }

    private void apply(BlockRequest r, Block b) {
        b.setName(r.name());
        b.setRegistration(r.registration());
        b.setArea(r.area());
    }

    private BlockResponse toResponse(Block b) {
        return new BlockResponse(
                b.getId(), b.getDevelopment().getId(), b.getDevelopment().getName(),
                b.getName(), b.getInternalCode(), b.getRegistration(), b.getArea(),
                lotRepository.countByBlockId(b.getId()));
    }
}
