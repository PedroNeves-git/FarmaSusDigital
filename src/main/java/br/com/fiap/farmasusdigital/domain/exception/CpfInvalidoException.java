package br.com.fiap.farmasusdigital.domain.exception;

public class CpfInvalidoException extends RuntimeException {

    public CpfInvalidoException(String cpf) {
        super("CPF invalido: " + cpf);
    }
}
