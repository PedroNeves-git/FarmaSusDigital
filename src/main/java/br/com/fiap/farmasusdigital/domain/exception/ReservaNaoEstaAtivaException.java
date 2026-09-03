package br.com.fiap.farmasusdigital.domain.exception;

public class ReservaNaoEstaAtivaException extends RuntimeException {

    public ReservaNaoEstaAtivaException(Long id) {
        super("Reserva nao esta ativa: " + id);
    }
}
