package br.com.fiap.farmasusdigital.domain.exception;

public class PacienteNaoEncontradoException extends RuntimeException {

    public PacienteNaoEncontradoException(String identificador) {
        super("Paciente nao encontrado: " + identificador);
    }
}
