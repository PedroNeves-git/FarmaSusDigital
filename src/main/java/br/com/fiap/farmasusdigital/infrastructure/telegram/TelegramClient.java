package br.com.fiap.farmasusdigital.infrastructure.telegram;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramGetUpdatesResponseDto;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramUpdateDto;

/**
 * Cliente HTTP sobre a Bot API do Telegram: envio de mensagens e, para uso
 * local sem webhook, busca de atualizacoes via long polling.
 */
@Component
public class TelegramClient {

    private final RestClient restClient;
    private final String botToken;

    public TelegramClient(@Value("${telegram.api.base-url:https://api.telegram.org}") String baseUrl,
                           @Value("${telegram.bot.token:}") String botToken) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.botToken = botToken;
    }

    public boolean tokenConfigurado() {
        return botToken != null && !botToken.isBlank();
    }

    public void enviarMensagem(Long chatId, String texto) {
        if (!tokenConfigurado()) {
            return;
        }
        restClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .body(Map.of("chat_id", chatId, "text", texto))
                .retrieve()
                .toBodilessEntity();
    }

    public List<TelegramUpdateDto> buscarAtualizacoes(long offset, int timeoutSegundos) {
        if (!tokenConfigurado()) {
            return List.of();
        }
        TelegramGetUpdatesResponseDto resposta = restClient.get()
                .uri("/bot{token}/getUpdates?offset={offset}&timeout={timeout}", botToken, offset, timeoutSegundos)
                .retrieve()
                .body(TelegramGetUpdatesResponseDto.class);
        return resposta == null || resposta.result() == null ? List.of() : resposta.result();
    }
}
