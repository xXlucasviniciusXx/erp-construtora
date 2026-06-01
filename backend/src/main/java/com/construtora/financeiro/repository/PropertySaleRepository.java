package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.PropertySale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PropertySaleRepository extends JpaRepository<PropertySale, UUID> {

    List<PropertySale> findByClientId(UUID clientId);

    @Query("""
            select s.lot.block.development.name as development,
                   count(s) as total,
                   coalesce(sum(s.totalValue), 0) as amount
            from PropertySale s
            group by s.lot.block.development.name
            """)
    List<Object[]> salesByDevelopment();
}
