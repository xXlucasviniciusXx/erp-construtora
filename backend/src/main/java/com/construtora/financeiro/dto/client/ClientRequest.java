package com.construtora.financeiro.dto.client;

import com.construtora.financeiro.model.enums.ClientStatus;
import com.construtora.financeiro.model.enums.PersonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotNull PersonType personType,
        @NotBlank String name,
        @NotBlank String document,
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
        ClientStatus status
) {}
