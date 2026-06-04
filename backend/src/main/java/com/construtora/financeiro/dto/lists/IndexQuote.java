package com.construtora.financeiro.dto.lists;

import java.util.UUID;

/**
 * Cotação oficial de um índice de correção, obtida do BCB (SGS).
 *
 * @param accumulated12m variação acumulada nos últimos 12 meses (%)
 * @param lastValue      variação do último mês divulgado (%)
 * @param lastRef        mês de referência do último valor (MM/AAAA)
 * @param available      false quando não há código SGS ou o BCB está indisponível
 */
public record IndexQuote(
        UUID id,
        String name,
        Integer sgsCode,
        Double accumulated12m,
        Double lastValue,
        String lastRef,
        boolean available
) {}
