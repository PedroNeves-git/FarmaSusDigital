package br.com.fiap.farmasusdigital.infrastructure.rest.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.fiap.farmasusdigital.domain.exception.CpfInvalidoException;
import br.com.fiap.farmasusdigital.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.farmasusdigital.domain.exception.MedicamentoNaoEncontradoException;
import br.com.fiap.farmasusdigital.domain.exception.PacienteJaPossuiReservaAtivaException;
import br.com.fiap.farmasusdigital.domain.exception.PacienteNaoEncontradoException;
import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEncontradaException;
import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEstaAtivaException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({PacienteNaoEncontradoException.class, MedicamentoNaoEncontradoException.class,
            ReservaNaoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(RuntimeException ex) {
        return corpoErro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({CpfInvalidoException.class})
    public ResponseEntity<Map<String, Object>> tratarRequisicaoInvalida(RuntimeException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({EstoqueInsuficienteException.class, PacienteJaPossuiReservaAtivaException.class,
            ReservaNaoEstaAtivaException.class})
    public ResponseEntity<Map<String, Object>> tratarConflito(RuntimeException ex) {
        return corpoErro(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Requisição inválida");
        return corpoErro(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<Map<String, Object>> corpoErro(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "mensagem", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
