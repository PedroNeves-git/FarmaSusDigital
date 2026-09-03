package br.com.fiap.farmasusdigital.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.usecase.EnviarLembretesExpiracaoUseCase;

@Component
public class LembreteExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(LembreteExpiracaoScheduler.class);

    private final EnviarLembretesExpiracaoUseCase enviarLembretesExpiracaoUseCase;

    public LembreteExpiracaoScheduler(EnviarLembretesExpiracaoUseCase enviarLembretesExpiracaoUseCase) {
        this.enviarLembretesExpiracaoUseCase = enviarLembretesExpiracaoUseCase;
    }

    @Scheduled(fixedRateString = "${reserva.lembrete.intervalo-ms:900000}")
    public void enviarLembretes() {
        int quantidade = enviarLembretesExpiracaoUseCase.executar();
        if (quantidade > 0) {
            log.info("{} lembrete(s) de expiracao de reserva enviado(s).", quantidade);
        }
    }
}
