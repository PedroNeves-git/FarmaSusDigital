package br.com.fiap.farmasusdigital.application.gateway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.fiap.farmasusdigital.domain.model.Reserva;

public interface ReservaGateway {

    Reserva salvar(Reserva reserva);

    Optional<Reserva> buscarPorId(Long id);

    Optional<Reserva> buscarAtivaPorPaciente(Long pacienteId);

    List<Reserva> buscarPorPaciente(Long pacienteId);

    List<Reserva> buscarAtivasVencidas(LocalDateTime agora);

    List<Reserva> buscarAtivasSemLembreteVencendoAte(LocalDateTime limite);
}
