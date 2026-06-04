package com.construtora.financeiro.scheduler;

import com.construtora.financeiro.repository.LotRepository;
import com.construtora.financeiro.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Jobs de manutenção:
 * <ul>
 *   <li>A cada 5 minutos: libera reservas de lotes vencidas.</li>
 *   <li>Diariamente: remove refresh tokens expirados há mais de 7 dias.</li>
 * </ul>
 */
@Component
public class ReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationScheduler.class);

    private final LotRepository lotRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public ReservationScheduler(LotRepository lotRepository,
                                RefreshTokenRepository refreshTokenRepository) {
        this.lotRepository = lotRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Libera lotes com reserva vencida a cada 5 minutos. */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void releaseExpiredReservations() {
        int released = lotRepository.releaseExpiredReservations(LocalDateTime.now());
        if (released > 0) {
            log.info("Reservas vencidas liberadas: {} lote(s)", released);
        }
    }

    /** Remove refresh tokens expirados há mais de 7 dias, todo dia à meia-noite. */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now().minusDays(7));
        if (deleted > 0) {
            log.info("Refresh tokens expirados removidos: {}", deleted);
        }
    }
}
