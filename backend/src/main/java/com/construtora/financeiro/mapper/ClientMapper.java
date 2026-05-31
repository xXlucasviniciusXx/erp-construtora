package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.client.ClientRequest;
import com.construtora.financeiro.dto.client.ClientResponse;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.enums.ClientStatus;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public Client toEntity(ClientRequest r, Client target) {
        Client c = target != null ? target : new Client();
        c.setPersonType(r.personType());
        c.setName(r.name());
        c.setDocument(r.document() == null ? null : r.document().replaceAll("\\D", ""));
        c.setStateRegistration(r.stateRegistration());
        c.setEmail(r.email());
        c.setPhone(r.phone());
        c.setAddress(r.address());
        c.setCity(r.city());
        c.setState(r.state());
        c.setZipCode(r.zipCode());
        c.setMaritalStatus(r.maritalStatus());
        c.setOccupation(r.occupation());
        c.setNotes(r.notes());
        c.setStatus(r.status() != null ? r.status() : ClientStatus.ACTIVE);
        return c;
    }

    public ClientResponse toResponse(Client c) {
        return new ClientResponse(
                c.getId(), c.getPersonType(), c.getName(), c.getDocument(), c.getStateRegistration(),
                c.getEmail(), c.getPhone(), c.getAddress(), c.getCity(), c.getState(), c.getZipCode(),
                c.getMaritalStatus(), c.getOccupation(), c.getNotes(), c.getStatus(), c.getCreatedAt());
    }
}
