package br.com.fiap.farmasusdigital.application.usecase;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.PacienteGateway;
import br.com.fiap.farmasusdigital.domain.model.Paciente;
import br.com.fiap.farmasusdigital.domain.service.CpfValidator;

@Component
public class BuscarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public BuscarPacienteUseCase(PacienteGateway pacienteGateway) {
        this.pacienteGateway = pacienteGateway;
    }

    /**
     * O identificador pode ser o CPF ou o telefone do paciente.
     */
    public Optional<Paciente> executarPorIdentificador(String identificador) {
        String somenteDigitos = CpfValidator.somenteDigitos(identificador);
        if (somenteDigitos != null && somenteDigitos.length() == 11) {
            Optional<Paciente> porCpf = pacienteGateway.buscarPorCpf(somenteDigitos);
            if (porCpf.isPresent()) {
                return porCpf;
            }
        }
        return pacienteGateway.buscarPorTelefone(identificador);
    }

    public Optional<Paciente> executarPorTelefone(String telefone) {
        return pacienteGateway.buscarPorTelefone(telefone);
    }
}
