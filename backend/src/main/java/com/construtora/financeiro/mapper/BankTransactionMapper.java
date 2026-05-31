package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.bank.BankTransactionResponse;
import com.construtora.financeiro.model.BankTransaction;
import org.springframework.stereotype.Component;

@Component
public class BankTransactionMapper {

    public BankTransactionResponse toResponse(BankTransaction t) {
        return new BankTransactionResponse(
                t.getId(), t.getBankAccount().getId(), t.getTransactionDate(), t.getDescription(),
                t.getAmount(), t.getType(), t.getDocumentNumber(), t.getBankIdentifier(), t.getStatus());
    }
}
