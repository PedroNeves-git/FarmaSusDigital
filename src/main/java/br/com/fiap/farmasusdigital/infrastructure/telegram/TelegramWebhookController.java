package br.com.fiap.farmasusdigital.infrastructure.telegram;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.farmasusdigital.application.usecase.ProcessarMensagemTelegramUseCase;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramMessageDto;
import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramUpdateDto;

// "telefone" do paciente e o chat id do Telegram, ja que a Bot API nao expoe
// o numero de celular sem compartilhamento de contato
@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    private final ProcessarMensagemTelegramUseCase processarMensagemTelegramUseCase;
    private final TelegramClient telegramClient;

    public TelegramWebhookController(ProcessarMensagemTelegramUseCase processarMensagemTelegramUseCase,
                                      TelegramClient telegramClient) {
        this.processarMensagemTelegramUseCase = processarMensagemTelegramUseCase;
        this.telegramClient = telegramClient;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receberUpdate(@RequestBody TelegramUpdateDto update) {
        TelegramMessageDto message = update.message();
        if (message == null || message.chat() == null || message.text() == null || message.text().isBlank()) {
            return ResponseEntity.ok().build();
        }

        Long chatId = message.chat().id();
        String telefone = chatId.toString();
        String resposta = processarMensagemTelegramUseCase.executar(telefone, message.text());
        telegramClient.enviarMensagem(chatId, resposta);

        return ResponseEntity.ok().build();
    }
}
