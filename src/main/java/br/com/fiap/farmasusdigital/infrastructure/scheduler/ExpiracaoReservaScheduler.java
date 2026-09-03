package br.com.fiap.farmasusdigital.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.usecase.ExpirarReservasVencidasUseCase;

/**
 * Job em background que libera periodicamente ao estoque fisico as reservas
 * que ultrapassaram o prazo de retirada (1 dia util).
 */
@Component
public class ExpiracaoReservaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoReservaScheduler.class);

    private final ExpirarReservasVencidasUseCase expirarReservasVencidasUseCase;

    public ExpiracaoReservaScheduler(ExpirarReservasVencidasUseCase expirarReservasVencidasUseCase) {
        this.expirarReservasVencidasUseCase = expirarReservasVencidasUseCase;
    }

    @Scheduled(fixedRateString = "${reserva.expiracao.intervalo-ms:900000}")
    public void expirarReservasVencidas() {
        int quantidade = expirarReservasVencidasUseCase.executar();
        if (quantidade > 0) {
            log.info("{} reserva(s) expirada(s) e liberada(s) de volta ao estoque.", quantidade);
        }
    }
}
