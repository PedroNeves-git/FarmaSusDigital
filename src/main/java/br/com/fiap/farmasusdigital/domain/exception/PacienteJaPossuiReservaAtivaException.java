package br.com.fiap.farmasusdigital.domain.exception;

public class PacienteJaPossuiReservaAtivaException extends RuntimeException {

    public PacienteJaPossuiReservaAtivaException(Long pacienteId) {
        super("Paciente ja possui uma reserva ativa: " + pacienteId);
    }
}
