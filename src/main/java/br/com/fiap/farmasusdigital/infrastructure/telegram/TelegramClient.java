package br.com.fiap.farmasusdigital.infrastructure.telegram;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramGetUpdatesResponseDto;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramUpdateDto;

/**
 * Cliente HTTP sobre a Bot API do Telegram: envio de mensagens (com botoes
 * inline quando ha opcoes) e, para uso local sem webhook, busca de
 * atualizacoes via long polling.
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

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
        enviarMensagem(chatId, texto, List.of());
    }

    /**
     * Envia a mensagem com botoes inline (anexados a propria mensagem) quando
     * ha opcoes de resposta rapida. Tocar num botao devolve o texto dele como
     * um callback, tratado da mesma forma que uma mensagem digitada.
     *
     * Uma falha ao entregar (chat invalido, bot bloqueado, instabilidade da
     * API) so e registrada em log - nunca propaga e derruba quem chamou,
     * porque a regra de negocio ja foi aplicada nesse ponto independente do
     * aviso ao paciente ter chegado ou nao.
     */
    public void enviarMensagem(Long chatId, String texto, List<String> opcoesRapidas) {
        if (!tokenConfigurado()) {
            return;
        }
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("chat_id", chatId);
        corpo.put("text", texto);
        corpo.put("parse_mode", "HTML");
        if (!opcoesRapidas.isEmpty()) {
            List<Map<String, String>> linha = opcoesRapidas.stream()
                    .map(opcao -> Map.of("text", opcao, "callback_data", opcao))
                    .toList();
            corpo.put("reply_markup", Map.of("inline_keyboard", List.of(linha)));
        }

        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao enviar mensagem ao chat {} via Telegram: {}", chatId, e.getMessage());
        }
    }

    /**
     * Confirma o toque no botao para o Telegram parar de mostrar o
     * carregamento nele. Precisa ser chamado sempre que um callback chega,
     * mas uma falha aqui (ex.: callback simulado/expirado) nao pode impedir
     * o processamento do clique em si.
     */
    public void responderCallback(String callbackQueryId) {
        if (!tokenConfigurado()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/bot{token}/answerCallbackQuery", botToken)
                    .body(Map.of("callback_query_id", callbackQueryId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao responder callback {} do Telegram: {}", callbackQueryId, e.getMessage());
        }
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
