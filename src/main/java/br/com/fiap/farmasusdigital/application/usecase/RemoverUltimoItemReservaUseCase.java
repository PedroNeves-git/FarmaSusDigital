package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEncontradaException;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

/**
 * Remove o ultimo medicamento adicionado a reserva em construcao, devolvendo
 * a unidade ao estoque. Se esse era o unico item, a reserva inteira e
 * cancelada (nao faz sentido manter uma reserva vazia).
 */
@Component
public class RemoverUltimoItemReservaUseCase {

    private final ReservaGateway reservaGateway;
    private final MedicamentoGateway medicamentoGateway;
    private final Clock clock;

    public RemoverUltimoItemReservaUseCase(ReservaGateway reservaGateway, MedicamentoGateway medicamentoGateway,
                                            Clock clock) {
        this.reservaGateway = reservaGateway;
        this.medicamentoGateway = medicamentoGateway;
        this.clock = clock;
    }

    public Resultado executar(Long reservaId) {
        Reserva reserva = reservaGateway.buscarPorId(reservaId)
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaId));

        ItemReserva itemRemovido = reserva.removerUltimoItem();
        medicamentoGateway.buscarPorId(itemRemovido.getMedicamentoId()).ifPresent(medicamento -> {
            medicamento.devolverAoEstoque(itemRemovido.getQuantidade());
            medicamentoGateway.salvar(medicamento);
        });

        if (reserva.getItens().isEmpty()) {
            reserva.cancelar(LocalDateTime.now(clock));
            reservaGateway.salvar(reserva);
            return new Resultado(itemRemovido, Optional.empty());
        }

        return new Resultado(itemRemovido, Optional.of(reservaGateway.salvar(reserva)));
    }

    /**
     * @param itemRemovido o item que acabou de ser retirado da reserva
     * @param reservaAtualizada a reserva apos a remocao, ou vazio se ela foi
     *                          cancelada por ter ficado sem nenhum item
     */
    public record Resultado(ItemReserva itemRemovido, Optional<Reserva> reservaAtualizada) {
    }
}
