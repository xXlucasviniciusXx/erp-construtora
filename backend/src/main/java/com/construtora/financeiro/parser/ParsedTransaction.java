package com.construtora.financeiro.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transação bancária neutra produzida por qualquer parser de extrato.
 * Desacopla o formato do arquivo (CSV/OFX/...) do restante da aplicação.
 */
public record ParsedTransaction(
        LocalDate date,
        String description,
        BigDecimal amount,      // positivo = crédito, negativo = débito
        String documentNumber,  // CPF/CNPJ encontrado, se houver
        String bankIdentifier   // identificador único do banco (FITID), se houver
) {}
