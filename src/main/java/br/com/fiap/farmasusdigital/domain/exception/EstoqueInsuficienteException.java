package br.com.fiap.farmasusdigital.domain.exception;

public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException(String nomeMedicamento) {
        super("Estoque insuficiente para o medicamento: " + nomeMedicamento);
    }
}
