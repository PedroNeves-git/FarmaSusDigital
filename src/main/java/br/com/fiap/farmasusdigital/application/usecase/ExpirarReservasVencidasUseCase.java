package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ExpirarReservasVencidasUseCase.class);

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

        int expiradas = 0;
        for (Reserva reserva : reservasVencidas) {
            try {
                reserva.expirar(agora);
                reserva.getItens().forEach(item ->
                        medicamentoGateway.buscarPorId(item.getMedicamentoId()).ifPresent(medicamento -> {
                            medicamento.devolverAoEstoque(item.getQuantidade());
                            medicamentoGateway.salvar(medicamento);
                        }));
                reservaGateway.salvar(reserva);
                expiradas++;
            } catch (RuntimeException e) {
                // isola a falha nessa reserva - as demais do lote continuam
                // sendo expiradas, e essa e retentada no proximo ciclo
                log.error("Falha ao expirar a reserva {}: {}", reserva.getId(), e.getMessage(), e);
            }
        }

        return expiradas;
    }
}
