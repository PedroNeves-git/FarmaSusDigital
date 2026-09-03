package br.com.fiap.farmasusdigital.application.gateway;

import java.util.Optional;

import br.com.fiap.farmasusdigital.domain.model.Paciente;

public interface PacienteGateway {

    Paciente salvar(Paciente paciente);

    Optional<Paciente> buscarPorTelefone(String telefone);

    Optional<Paciente> buscarPorCpf(String cpf);

    Optional<Paciente> buscarPorId(Long id);
}
