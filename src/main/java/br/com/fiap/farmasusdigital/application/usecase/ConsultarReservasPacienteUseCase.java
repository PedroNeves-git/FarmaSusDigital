package br.com.fiap.farmasusdigital.application.usecase;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

@Component
public class ConsultarReservasPacienteUseCase {

    private final ReservaGateway reservaGateway;

    public ConsultarReservasPacienteUseCase(ReservaGateway reservaGateway) {
        this.reservaGateway = reservaGateway;
    }

    public List<Reserva> executar(Long pacienteId) {
        return reservaGateway.buscarPorPaciente(pacienteId);
    }
}
