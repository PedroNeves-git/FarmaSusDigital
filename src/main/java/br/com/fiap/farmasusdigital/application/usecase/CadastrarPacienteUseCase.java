package br.com.fiap.farmasusdigital.application.usecase;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.PacienteGateway;
import br.com.fiap.farmasusdigital.domain.exception.CpfInvalidoException;
import br.com.fiap.farmasusdigital.domain.model.Paciente;
import br.com.fiap.farmasusdigital.domain.service.CpfValidator;
import br.com.fiap.farmasusdigital.domain.service.NomeFormatter;

@Component
public class CadastrarPacienteUseCase {

    private final PacienteGateway pacienteGateway;

    public CadastrarPacienteUseCase(PacienteGateway pacienteGateway) {
        this.pacienteGateway = pacienteGateway;
    }

    public Paciente executar(String cpf, String nomeCompleto, String telefone) {
        if (!CpfValidator.isValido(cpf)) {
            throw new CpfInvalidoException(cpf);
        }
        String cpfNormalizado = CpfValidator.somenteDigitos(cpf);
        Paciente paciente = new Paciente(null, cpfNormalizado, NomeFormatter.capitalizar(nomeCompleto), telefone);
        return pacienteGateway.salvar(paciente);
    }
}
