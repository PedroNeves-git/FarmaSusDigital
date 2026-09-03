package br.com.fiap.farmasusdigital.domain.exception;

public class MedicamentoNaoEncontradoException extends RuntimeException {

    public MedicamentoNaoEncontradoException(String nome) {
        super("Medicamento nao encontrado: " + nome);
    }
}
