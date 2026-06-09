package com.construtora.financeiro.service.distrato;

import com.construtora.financeiro.dto.distrato.DistratoConfigRequest;
import com.construtora.financeiro.dto.distrato.DistratoConfigResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.DistratoMapper;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.DistratoConfig;
import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import com.construtora.financeiro.repository.DevelopmentRepository;
import com.construtora.financeiro.repository.DistratoConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DistratoConfigService {

    /** Regra adotada quando não há nenhuma configuração cadastrada. */
    public static final DistratoFinancialRule DEFAULT_RULE = DistratoFinancialRule.RETENCAO_MAIS_PARCELAS_VENCIDAS;

    private final DistratoConfigRepository repository;
    private final DevelopmentRepository developmentRepository;
    private final com.construtora.financeiro.security.DevelopmentScopeService scope;
    private final DistratoMapper mapper;

    public DistratoConfigService(DistratoConfigRepository repository,
                                 DevelopmentRepository developmentRepository,
                                 com.construtora.financeiro.security.DevelopmentScopeService scope,
                                 DistratoMapper mapper) {
        this.repository = repository;
        this.developmentRepository = developmentRepository;
        this.scope = scope;
        this.mapper = mapper;
    }

    /**
     * Regra financeira efetiva para um empreendimento: usa a configuração
     * específica do empreendimento; na ausência, a global; na ausência de
     * ambas, a regra padrão recomendada.
     */
    @Transactional(readOnly = true)
    public DistratoFinancialRule resolveRule(UUID developmentId) {
        if (developmentId != null) {
            var specific = repository.findByDevelopmentId(developmentId);
            if (specific.isPresent() && specific.get().isActive()) {
                return specific.get().getFinancialRule();
            }
        }
        return repository.findByDevelopmentIsNull()
                .map(DistratoConfig::getFinancialRule)
                .orElse(DEFAULT_RULE);
    }

    @Transactional(readOnly = true)
    public List<DistratoConfigResponse> list() {
        return repository.findAllByOrderByDevelopmentIdAsc().stream()
                // global (sem empreendimento) sempre visível; específicas só dentro do escopo
                .filter(c -> c.getDevelopment() == null || scope.canAccess(c.getDevelopment().getId()))
                .map(mapper::toResponse).toList();
    }

    public DistratoConfigResponse upsert(DistratoConfigRequest request) {
        DistratoConfig config = (request.developmentId() == null
                ? repository.findByDevelopmentIsNull()
                : repository.findByDevelopmentId(request.developmentId()))
                .orElseGet(DistratoConfig::new);

        if (request.developmentId() != null && config.getDevelopment() == null) {
            Development dev = developmentRepository.findById(request.developmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Empreendimento", request.developmentId()));
            config.setDevelopment(dev);
        }
        config.setFinancialRule(request.financialRule());
        config.setActive(true);
        return mapper.toResponse(repository.save(config));
    }

    public void delete(UUID id) {
        DistratoConfig config = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Configuração de distrato", id));
        if (config.getDevelopment() == null) {
            throw new com.construtora.financeiro.exception.BusinessException(
                    "A configuração global não pode ser removida.");
        }
        repository.delete(config);
    }
}
