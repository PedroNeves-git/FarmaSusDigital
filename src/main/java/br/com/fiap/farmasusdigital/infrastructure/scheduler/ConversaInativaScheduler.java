package br.com.fiap.farmasusdigital.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.usecase.ExpirarConversasInativasUseCase;

@Component
public class ConversaInativaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConversaInativaScheduler.class);

    private final ExpirarConversasInativasUseCase expirarConversasInativasUseCase;

    public ConversaInativaScheduler(ExpirarConversasInativasUseCase expirarConversasInativasUseCase) {
        this.expirarConversasInativasUseCase = expirarConversasInativasUseCase;
    }

    @Scheduled(fixedRateString = "${conversa.timeout-verificacao-intervalo-ms:600000}")
    public void expirarConversasInativas() {
        int quantidade = expirarConversasInativasUseCase.executar();
        if (quantidade > 0) {
            log.info("{} conversa(s) inativa(s) do bot foram encerradas por timeout.", quantidade);
        }
    }
}
