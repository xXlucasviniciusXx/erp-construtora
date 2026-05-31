package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.client.ClientRequest;
import com.construtora.financeiro.dto.client.ClientResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.ClientMapper;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ClientService {

    private final ClientRepository repository;
    private final ClientMapper mapper;

    public ClientService(ClientRepository repository, ClientMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String query, Pageable pageable) {
        Page<Client> page = (query == null || query.isBlank())
                ? repository.findAll(pageable)
                : repository.findByNameContainingIgnoreCaseOrDocumentContaining(query, query, pageable);
        return page.map(mapper::toResponse);
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
        return mapper.toResponse(repository.save(mapper.toEntity(request, null)));
    }

    public ClientResponse update(UUID id, ClientRequest request) {
        Client client = getEntity(id);
        return mapper.toResponse(repository.save(mapper.toEntity(request, client)));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public Client getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }
}
