package com.construtora.financeiro.scheduler;

import com.construtora.financeiro.service.InstallmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diário que marca parcelas vencidas como atrasadas e dispara notificações.
 * TODO: tornar o horário configurável e adicionar lock para múltiplas instâncias.
 */
@Component
public class OverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueScheduler.class);

    private final InstallmentService installmentService;

    public OverdueScheduler(InstallmentService installmentService) {
        this.installmentService = installmentService;
    }

    // Todo dia às 06:00 (horário do servidor)
    @Scheduled(cron = "0 0 6 * * *")
    public void flagOverdueInstallments() {
        int count = installmentService.markOverdue();
        if (count > 0) {
            log.info("{} parcela(s) marcada(s) como atrasada(s)", count);
        }
    }

    // Todo dia às 06:30 — lembretes de vencimento próximo (N dias configurável)
    @Scheduled(cron = "0 30 6 * * *")
    public void sendDueSoonReminders() {
        int count = installmentService.remindDueSoon();
        if (count > 0) {
            log.info("{} lembrete(s) de vencimento enviado(s)", count);
        }
    }
}
