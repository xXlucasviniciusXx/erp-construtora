package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.client.ClientRequest;
import com.construtora.financeiro.dto.client.ClientResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.ClientMapper;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.enums.ClientStatus;
import com.construtora.financeiro.model.enums.ReceivableStatus;
import com.construtora.financeiro.repository.AccountReceivableRepository;
import com.construtora.financeiro.repository.ClientRepository;
import com.construtora.financeiro.repository.InstallmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ClientService {

    private final ClientRepository repository;
    private final InstallmentRepository installmentRepository;
    private final AccountReceivableRepository receivableRepository;
    private final AuditService auditService;
    private final ClientMapper mapper;

    public ClientService(ClientRepository repository,
                         InstallmentRepository installmentRepository,
                         AccountReceivableRepository receivableRepository,
                         AuditService auditService,
                         ClientMapper mapper) {
        this.repository = repository;
        this.installmentRepository = installmentRepository;
        this.receivableRepository = receivableRepository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String query, com.construtora.financeiro.model.enums.ClientStatus status, Pageable pageable) {
        String q = (query != null && !query.isBlank()) ? query.trim() : "";
        return repository.search(q, status, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    public ClientResponse create(ClientRequest request) {
        String document = request.document().replaceAll("\\D", "");
        if (repository.existsByDocument(document)) {
            throw new BusinessException("Já existe cliente com este CPF/CNPJ");
        }
        Client saved = repository.save(mapper.toEntity(request, null));
        auditService.log("CLIENT_CREATE", "clients", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public ClientResponse update(UUID id, ClientRequest request) {
        Client client = getEntity(id);
        Client saved = repository.save(mapper.toEntity(request, client));
        auditService.log("CLIENT_UPDATE", "clients", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    /**
     * Inativação lógica (soft delete). Bloqueada se o cliente possuir débitos
     * pendentes (parcelas em aberto/atrasadas ou contas a receber em aberto).
     */
    public ClientResponse inactivate(UUID id) {
        Client client = getEntity(id);
        if (hasOpenDebts(id)) {
            throw new BusinessException(
                    "Não é possível inativar este cliente pois existem débitos pendentes.");
        }
        client.setStatus(ClientStatus.INACTIVE);
        Client saved = repository.save(client);
        auditService.log("CLIENT_INACTIVATE", "clients", id, client.getName());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean hasOpenDebts(UUID clientId) {
        return installmentRepository.countOpenDebtsByClient(clientId) > 0
                || receivableRepository.existsByClientIdAndStatus(clientId, ReceivableStatus.OPEN);
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public Client getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }
}
