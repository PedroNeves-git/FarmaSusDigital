package br.com.fiap.farmasusdigital.infrastructure.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.farmasusdigital.application.usecase.BuscarPacienteUseCase;
import br.com.fiap.farmasusdigital.domain.exception.PacienteNaoEncontradoException;
import br.com.fiap.farmasusdigital.infrastructure.rest.dto.PacienteResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final BuscarPacienteUseCase buscarPacienteUseCase;

    public PacienteController(BuscarPacienteUseCase buscarPacienteUseCase) {
        this.buscarPacienteUseCase = buscarPacienteUseCase;
    }

    @GetMapping("/{identificador}")
    public ResponseEntity<PacienteResponse> buscar(
            @Parameter(example = "52998224725", description = "CPF ou telefone do paciente")
            @PathVariable String identificador) {
        return buscarPacienteUseCase.executarPorIdentificador(identificador)
                .map(PacienteResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PacienteNaoEncontradoException(identificador));
    }
}
