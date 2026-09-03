package br.com.fiap.farmasusdigital.infrastructure.telegram;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.NotificacaoGateway;

// "telefone" do paciente e o chat id do Telegram (ver TelegramWebhookController)
@Component
public class TelegramNotificacaoGatewayImpl implements NotificacaoGateway {

    private final TelegramClient telegramClient;

    public TelegramNotificacaoGatewayImpl(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void enviarMensagem(String telefone, String mensagem) {
        telegramClient.enviarMensagem(Long.valueOf(telefone), mensagem);
    }
}
