package br.com.fiap.farmasusdigital.infrastructure.telegram;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import br.com.fiap.farmasusdigital.application.usecase.ProcessarMensagemTelegramUseCase;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramMessageDto;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramUpdateDto;

/**
 * Alternativa ao webhook para uso local: consulta a API do Telegram
 * periodicamente em vez de exigir uma URL publica. Nao usar junto com um
 * webhook configurado no bot - sao mutuamente exclusivos na API do Telegram.
 */
@Component
@ConditionalOnProperty(prefix = "telegram.polling", name = "enabled", havingValue = "true")
public class TelegramPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollingScheduler.class);
    private static final int TIMEOUT_LONG_POLLING_SEGUNDOS = 30;

    private final TelegramClient telegramClient;
    private final ProcessarMensagemTelegramUseCase processarMensagemTelegramUseCase;
    private final AtomicLong proximoOffset = new AtomicLong(0);

    public TelegramPollingScheduler(TelegramClient telegramClient,
                                     ProcessarMensagemTelegramUseCase processarMensagemTelegramUseCase) {
        this.telegramClient = telegramClient;
        this.processarMensagemTelegramUseCase = processarMensagemTelegramUseCase;
    }

    @Scheduled(fixedDelayString = "${telegram.polling.interval-ms:500}")
    public void buscarEProcessarMensagens() {
        if (!telegramClient.tokenConfigurado()) {
            return;
        }

        List<TelegramUpdateDto> atualizacoes;
        try {
            atualizacoes = telegramClient.buscarAtualizacoes(proximoOffset.get(), TIMEOUT_LONG_POLLING_SEGUNDOS);
        } catch (RestClientException e) {
            log.warn("Falha ao buscar atualizacoes do Telegram via polling: {}", e.getMessage());
            return;
        }

        for (TelegramUpdateDto update : atualizacoes) {
            proximoOffset.set(update.updateId() + 1);
            processarUpdate(update);
        }
    }

    private void processarUpdate(TelegramUpdateDto update) {
        TelegramMessageDto message = update.message();
        if (message == null || message.chat() == null || message.text() == null || message.text().isBlank()) {
            return;
        }

        Long chatId = message.chat().id();
        String telefone = chatId.toString();
        String resposta = processarMensagemTelegramUseCase.executar(telefone, message.text());
        telegramClient.enviarMensagem(chatId, resposta);
    }
}
