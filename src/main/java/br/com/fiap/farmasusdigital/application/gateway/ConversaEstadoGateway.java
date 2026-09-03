package br.com.fiap.farmasusdigital.application.gateway;

import java.time.LocalDateTime;
import java.util.Optional;

import br.com.fiap.farmasusdigital.domain.model.ConversaEstado;

public interface ConversaEstadoGateway {

    ConversaEstado salvar(ConversaEstado conversaEstado);

    Optional<ConversaEstado> buscarPorTelefone(String telefone);

    void remover(String telefone);

    int removerInativasAntesDe(LocalDateTime limite);
}
