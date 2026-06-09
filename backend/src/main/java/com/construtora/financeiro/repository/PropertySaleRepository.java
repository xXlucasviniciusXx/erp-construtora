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

    /**
     * Vendas vigentes (não canceladas) de um empreendimento, com cliente carregado.
     * Usada para anexar o comprador atual a cada lote na tela de Lotes — um lote
     * vendido pode ter venda ACTIVE (em pagamento) ou COMPLETED (quitada); apenas o
     * distrato (CANCELLED) desvincula o comprador.
     */
    @Query("""
            select s from PropertySale s
            join fetch s.client
            where s.lot.block.development.id = :developmentId
              and s.status <> com.construtora.financeiro.model.enums.SaleStatus.CANCELLED
            """)
    List<PropertySale> findCurrentSalesByDevelopment(@Param("developmentId") UUID developmentId);

    /** Próximo número de contrato a partir da sequência do banco. */
    @Query(value = "SELECT nextval('contract_number_seq')", nativeQuery = true)
    long nextContractSequence();

    /**
     * Busca paginada de vendas: por cliente, status e texto (cliente/empreendimento/quadra/lote).
     * Escopo de empreendimento: quando {@code unrestricted=false}, restringe a {@code devIds}.
     */
    @Query(value = """
            select s from PropertySale s
            where (:clientId is null or s.client.id = :clientId)
              and (:status is null or s.status = :status)
              and (:unrestricted = true or s.lot.block.development.id in :devIds)
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
              and (:unrestricted = true or s.lot.block.development.id in :devIds)
              and (:q = ''
                   or lower(s.client.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.block.name) like lower(concat('%', :q, '%'))
                   or lower(s.lot.block.development.name) like lower(concat('%', :q, '%')))
            """)
    Page<PropertySale> search(@Param("q") String q,
                              @Param("status") SaleStatus status,
                              @Param("clientId") UUID clientId,
                              @Param("unrestricted") boolean unrestricted,
                              @Param("devIds") java.util.Collection<UUID> devIds,
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
