package br.com.fiap.farmasusdigital.application.gateway;

public interface NotificacaoGateway {

    void enviarMensagem(String telefone, String mensagem);
}
