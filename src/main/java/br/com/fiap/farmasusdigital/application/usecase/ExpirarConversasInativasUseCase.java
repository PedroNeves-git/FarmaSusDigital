package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.ConversaEstadoGateway;

/**
 * Encerra conversas do bot sem atividade recente, para o paciente nao ficar
 * preso numa etapa antiga se voltar a falar dias depois.
 */
@Component
public class ExpirarConversasInativasUseCase {

    private final ConversaEstadoGateway conversaEstadoGateway;
    private final Clock clock;
    private final long timeoutMinutos;

    public ExpirarConversasInativasUseCase(ConversaEstadoGateway conversaEstadoGateway, Clock clock,
                                            @Value("${conversa.timeout-minutos:30}") long timeoutMinutos) {
        this.conversaEstadoGateway = conversaEstadoGateway;
        this.clock = clock;
        this.timeoutMinutos = timeoutMinutos;
    }

    public int executar() {
        LocalDateTime limite = LocalDateTime.now(clock).minusMinutes(timeoutMinutos);
        return conversaEstadoGateway.removerInativasAntesDe(limite);
    }
}
