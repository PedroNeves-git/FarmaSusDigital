package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEncontradaException;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

@Component
public class CancelarReservaUseCase {

    private final ReservaGateway reservaGateway;
    private final MedicamentoGateway medicamentoGateway;
    private final Clock clock;

    public CancelarReservaUseCase(ReservaGateway reservaGateway, MedicamentoGateway medicamentoGateway, Clock clock) {
        this.reservaGateway = reservaGateway;
        this.medicamentoGateway = medicamentoGateway;
        this.clock = clock;
    }

    public Reserva executar(Long reservaId) {
        Reserva reserva = reservaGateway.buscarPorId(reservaId)
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaId));

        reserva.cancelar(LocalDateTime.now(clock));
        devolverItensAoEstoque(reserva);
        return reservaGateway.salvar(reserva);
    }

    private void devolverItensAoEstoque(Reserva reserva) {
        reserva.getItens().forEach(item ->
                medicamentoGateway.buscarPorId(item.getMedicamentoId()).ifPresent(medicamento -> {
                    medicamento.devolverAoEstoque(item.getQuantidade());
                    medicamentoGateway.salvar(medicamento);
                }));
    }
}
