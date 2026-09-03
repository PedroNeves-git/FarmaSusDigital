package br.com.fiap.farmasusdigital.infrastructure.rest.dto;

import br.com.fiap.farmasusdigital.domain.model.Paciente;

public record PacienteResponse(Long id, String cpf, String nomeCompleto, String telefone) {

    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(paciente.getId(), paciente.getCpf(), paciente.getNomeCompleto(), paciente.getTelefone());
    }
}
