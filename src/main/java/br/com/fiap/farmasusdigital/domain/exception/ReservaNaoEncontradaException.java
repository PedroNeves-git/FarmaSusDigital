package br.com.fiap.farmasusdigital.domain.exception;

public class ReservaNaoEncontradaException extends RuntimeException {

    public ReservaNaoEncontradaException(Long id) {
        super("Reserva nao encontrada: " + id);
    }
}
