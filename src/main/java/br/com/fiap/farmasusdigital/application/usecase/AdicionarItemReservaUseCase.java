package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.MedicamentoNaoEncontradoException;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

/**
 * Adiciona um medicamento a reserva ativa do paciente, criando a reserva
 * (respeitando o limite de 1 reserva ativa por pessoa) caso ainda nao exista.
 */
@Component
public class AdicionarItemReservaUseCase {

    private final ReservaGateway reservaGateway;
    private final MedicamentoGateway medicamentoGateway;
    private final Clock clock;

    public AdicionarItemReservaUseCase(ReservaGateway reservaGateway, MedicamentoGateway medicamentoGateway, Clock clock) {
        this.reservaGateway = reservaGateway;
        this.medicamentoGateway = medicamentoGateway;
        this.clock = clock;
    }

    public Reserva executar(Long pacienteId, Long medicamentoId, int quantidade) {
        Medicamento medicamento = medicamentoGateway.buscarPorId(medicamentoId)
                .orElseThrow(() -> new MedicamentoNaoEncontradoException(String.valueOf(medicamentoId)));

        medicamento.reservar(quantidade);
        medicamentoGateway.salvar(medicamento);

        Reserva reserva = buscarOuCriarReservaAtiva(pacienteId);
        reserva.adicionarItem(new ItemReserva(null, medicamento.getId(), medicamento.getNome(), quantidade));
        return reservaGateway.salvar(reserva);
    }

    private Reserva buscarOuCriarReservaAtiva(Long pacienteId) {
        Optional<Reserva> reservaAtiva = reservaGateway.buscarAtivaPorPaciente(pacienteId);
        if (reservaAtiva.isPresent()) {
            return reservaAtiva.get();
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        Reserva novaReserva = Reserva.abrirNova(pacienteId, agora);
        return reservaGateway.salvar(novaReserva);
    }
}
