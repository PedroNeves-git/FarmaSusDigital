package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

/**
 * Executado periodicamente (scheduler) para liberar de volta ao estoque
 * fisico as reservas que nao foram retiradas dentro do prazo.
 */
@Component
public class ExpirarReservasVencidasUseCase {

    private final ReservaGateway reservaGateway;
    private final MedicamentoGateway medicamentoGateway;
    private final Clock clock;

    public ExpirarReservasVencidasUseCase(ReservaGateway reservaGateway, MedicamentoGateway medicamentoGateway, Clock clock) {
        this.reservaGateway = reservaGateway;
        this.medicamentoGateway = medicamentoGateway;
        this.clock = clock;
    }

    public int executar() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Reserva> reservasVencidas = reservaGateway.buscarAtivasVencidas(agora);

        for (Reserva reserva : reservasVencidas) {
            reserva.expirar(agora);
            reserva.getItens().forEach(item ->
                    medicamentoGateway.buscarPorId(item.getMedicamentoId()).ifPresent(medicamento -> {
                        medicamento.devolverAoEstoque(item.getQuantidade());
                        medicamentoGateway.salvar(medicamento);
                    }));
            reservaGateway.salvar(reserva);
        }

        return reservasVencidas.size();
    }
}
