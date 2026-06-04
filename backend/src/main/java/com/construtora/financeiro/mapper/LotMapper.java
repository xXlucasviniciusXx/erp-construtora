package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.lot.LotResponse;
import com.construtora.financeiro.model.Block;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.Lot;
import org.springframework.stereotype.Component;

@Component
public class LotMapper {

    public LotResponse toResponse(Lot l) {
        Block block = l.getBlock();
        Development dev = block.getDevelopment();
        return new LotResponse(
                l.getId(), block.getId(), block.getName(), dev.getId(), dev.getName(),
                l.getName(), l.getInternalCode(), l.getRegistration(), l.getUnit(), l.getAddress(),
                l.getTotalArea(), l.getBuiltArea(), l.getPlannedValue(), l.getSaleValue(),
                l.getStatus(), l.getContractExtra(), l.getNotes(), label(l),
                l.getReservationExpiresAt());
    }

    /** Rótulo "Empreendimento / Q<quadra> / <lote>" usado na venda e no contrato. */
    public static String label(Lot l) {
        Block block = l.getBlock();
        return block.getDevelopment().getName() + " / " + block.getName() + " / " + l.getName();
    }
}
