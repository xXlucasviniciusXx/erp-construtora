package com.construtora.financeiro.dto.client;

import com.construtora.financeiro.model.enums.ClientStatus;
import com.construtora.financeiro.model.enums.PersonType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        PersonType personType,
        String name,
        String document,
        String stateRegistration,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String zipCode,
        String maritalStatus,
        String occupation,
        String notes,
        ClientStatus status,
        OffsetDateTime createdAt
) {}
