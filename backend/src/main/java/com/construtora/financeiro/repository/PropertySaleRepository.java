package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PropertySaleRepository extends JpaRepository<PropertySale, UUID> {

    List<PropertySale> findByClientId(UUID clientId);

    /** Próximo número de contrato a partir da sequência do banco. */
    @Query(value = "SELECT nextval('contract_number_seq')", nativeQuery = true)
    long nextContractSequence();

    /** Busca paginada de vendas: por cliente, status e texto (cliente/empreendimento/quadra/lote). */
    @Query(value = """
            select s from PropertySale s
            where (:clientId is null or s.client.id = :clientId)
              and (:status is null or s.status = :status)
              and (:q = ''
                   or lower(s.client.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.block.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.block.development.name) like lower(concat('%', :q, '%')))
            """,
            countQuery = """
            select count(s) from PropertySale s
            where (:clientId is null or s.client.id = :clientId)
              and (:status is null or s.status = :status)
              and (:q = ''
                   or lower(s.client.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.block.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.block.development.name) like lower(concat('%', :q, '%')))
            """)
    Page<PropertySale> search(@Param("q") String q,
                              @Param("status") SaleStatus status,
                              @Param("clientId") UUID clientId,
                              Pageable pageable);

    @Query("""
            select s.lot.block.development.name as development,
                   count(s) as total,
                   coalesce(sum(s.totalValue), 0) as amount
            from PropertySale s
            group by s.lot.block.development.name
            """)
    List<Object[]> salesByDevelopment();
}
