package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEncontradaException;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

/**
 * Confirma a retirada fisica do medicamento no balcao, encerrando a reserva.
 */
@Component
public class RetirarReservaUseCase {

    private final ReservaGateway reservaGateway;
    private final Clock clock;

    public RetirarReservaUseCase(ReservaGateway reservaGateway, Clock clock) {
        this.reservaGateway = reservaGateway;
        this.clock = clock;
    }

    public Reserva executar(Long reservaId) {
        Reserva reserva = reservaGateway.buscarPorId(reservaId)
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaId));

        reserva.retirar(LocalDateTime.now(clock));
        return reservaGateway.salvar(reserva);
    }
}
