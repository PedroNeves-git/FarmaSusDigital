package br.com.fiap.farmasusdigital.infrastructure.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.usecase.ProcessarMensagemTelegramUseCase;
import br.com.fiap.farmasusdigital.application.usecase.RespostaConversa;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramCallbackQueryDto;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramMessageDto;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramUpdateDto;

/**
 * Processa um update do Telegram vindo tanto do webhook quanto do polling:
 * uma mensagem de texto normal, ou o toque num botao inline (callback_query).
 */
@Component
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private final ProcessarMensagemTelegramUseCase processarMensagemTelegramUseCase;
    private final TelegramClient telegramClient;

    public TelegramUpdateHandler(ProcessarMensagemTelegramUseCase processarMensagemTelegramUseCase,
                                  TelegramClient telegramClient) {
        this.processarMensagemTelegramUseCase = processarMensagemTelegramUseCase;
        this.telegramClient = telegramClient;
    }

    /**
     * Nunca deixa uma excecao escapar daqui: no webhook, isso viraria um 500
     * que faz o Telegram reentregar o mesmo update repetidamente; no polling,
     * derrubaria o processamento dos updates seguintes do mesmo lote (o
     * offset ja teria avancado, entao esse update tambem nunca seria
     * reprocessado). Uma falha pontual num update fica isolada nele mesmo.
     */
    public void tratar(TelegramUpdateDto update) {
        try {
            if (update.callbackQuery() != null) {
                tratarCallback(update.callbackQuery());
                return;
            }
            tratarMensagem(update.message());
        } catch (RuntimeException e) {
            log.error("Falha ao processar update {} do Telegram: {}", update.updateId(), e.getMessage(), e);
        }
    }

    private void tratarMensagem(TelegramMessageDto message) {
        if (message == null || message.chat() == null || message.text() == null || message.text().isBlank()) {
            return;
        }
        processarEResponder(message.chat().id(), message.text());
    }

    private void tratarCallback(TelegramCallbackQueryDto callback) {
        telegramClient.responderCallback(callback.id());
        if (callback.message() == null || callback.message().chat() == null || callback.data() == null) {
            return;
        }
        processarEResponder(callback.message().chat().id(), callback.data());
    }

    private void processarEResponder(Long chatId, String texto) {
        String telefone = chatId.toString();
        RespostaConversa resposta = processarMensagemTelegramUseCase.executar(telefone, texto);
        telegramClient.enviarMensagem(chatId, resposta.mensagem(), resposta.opcoesRapidas());
    }
}
