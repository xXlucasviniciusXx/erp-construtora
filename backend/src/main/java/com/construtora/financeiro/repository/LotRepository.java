package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Lot;
import com.construtora.financeiro.model.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LotRepository extends JpaRepository<Lot, UUID> {

    long countByStatus(PropertyStatus status);

    List<Lot> findByBlockIdOrderByInternalCode(UUID blockId);

    long countByBlockId(UUID blockId);

    long countByBlockDevelopmentId(UUID developmentId);

    List<Lot> findByBlockDevelopmentIdOrderByInternalCode(UUID developmentId);

    /** Valor previsto total do empreendimento = soma dos valores previstos dos lotes. */
    @Query("select coalesce(sum(l.plannedValue), 0) from Lot l where l.block.development.id = :devId")
    BigDecimal plannedTotalByDevelopment(@Param("devId") UUID devId);

    /** Valor realmente recebido = soma dos valores de venda dos lotes vendidos. */
    @Query("""
            select coalesce(sum(l.saleValue), 0) from Lot l
            where l.block.development.id = :devId
              and l.status = com.construtora.financeiro.model.enums.PropertyStatus.SOLD
            """)
    BigDecimal receivedTotalByDevelopment(@Param("devId") UUID devId);

    /**
     * Libera reservas vencidas: status RESERVED → AVAILABLE, limpa expiresAt.
     * Retorna o número de lotes liberados.
     */
    @Modifying
    @Query("""
            UPDATE Lot l
            SET l.status = com.construtora.financeiro.model.enums.PropertyStatus.AVAILABLE,
                l.reservationExpiresAt = null
            WHERE l.status = com.construtora.financeiro.model.enums.PropertyStatus.RESERVED
              AND l.reservationExpiresAt IS NOT NULL
              AND l.reservationExpiresAt < :now
            """)
    int releaseExpiredReservations(@Param("now") LocalDateTime now);
}
