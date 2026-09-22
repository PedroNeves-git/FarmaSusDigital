package br.com.fiap.farmasusdigital.infrastructure.telegram;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.farmasusdigital.infrastructure.telegram.dto.TelegramUpdateDto;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

// "telefone" do paciente e o chat id do Telegram, ja que a Bot API nao expoe
// o numero de celular sem compartilhamento de contato
@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    private final TelegramUpdateHandler telegramUpdateHandler;

    public TelegramWebhookController(TelegramUpdateHandler telegramUpdateHandler) {
        this.telegramUpdateHandler = telegramUpdateHandler;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receberUpdate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "update_id": 1,
                              "message": {
                                "chat": { "id": 999999 },
                                "text": "oi"
                              }
                            }
                            """)))
            @RequestBody TelegramUpdateDto update) {
        telegramUpdateHandler.tratar(update);
        return ResponseEntity.ok().build();
    }
}
