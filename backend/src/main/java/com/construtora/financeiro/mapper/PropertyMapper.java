package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.property.PropertyRequest;
import com.construtora.financeiro.dto.property.PropertyResponse;
import com.construtora.financeiro.model.Property;
import com.construtora.financeiro.model.enums.PropertyStatus;
import org.springframework.stereotype.Component;

@Component
public class PropertyMapper {

    public Property toEntity(PropertyRequest r, Property target) {
        Property p = target != null ? target : new Property();
        p.setDevelopment(r.development());
        p.setBlock(r.block());
        p.setLot(r.lot());
        p.setUnit(r.unit());
        p.setRegistration(r.registration());
        p.setAddress(r.address());
        p.setTotalArea(r.totalArea());
        p.setBuiltArea(r.builtArea());
        p.setSaleValue(r.saleValue());
        if (r.status() != null) {
            p.setStatus(r.status());
        } else if (p.getStatus() == null) {
            p.setStatus(PropertyStatus.AVAILABLE);
        }
        p.setContractExtra(r.contractExtra());
        p.setNotes(r.notes());
        return p;
    }

    public PropertyResponse toResponse(Property p) {
        return new PropertyResponse(
                p.getId(), p.getDevelopment(), p.getBlock(), p.getLot(), p.getUnit(),
                p.getRegistration(), p.getAddress(), p.getTotalArea(), p.getBuiltArea(),
                p.getSaleValue(), p.getStatus(), p.getContractExtra(), p.getNotes(), p.getCreatedAt());
    }
}
