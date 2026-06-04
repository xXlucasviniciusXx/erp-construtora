package com.construtora.financeiro.dto.lot;

/**
 * Reserva um lote disponível por {@code hours} horas (padrão: 24 h).
 * Passar {@code null} ou omitir usa o padrão.
 */
public record LotReserveRequest(Integer hours) {
    public int resolvedHours() { return hours != null && hours > 0 ? hours : 24; }
}
