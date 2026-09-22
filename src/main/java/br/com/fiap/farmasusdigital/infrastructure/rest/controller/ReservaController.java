package br.com.fiap.farmasusdigital.infrastructure.rest.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.farmasusdigital.application.usecase.CancelarReservaUseCase;
import br.com.fiap.farmasusdigital.application.usecase.ConsultarReservasPacienteUseCase;
import br.com.fiap.farmasusdigital.application.usecase.RetirarReservaUseCase;
import br.com.fiap.farmasusdigital.infrastructure.rest.dto.ReservaResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ConsultarReservasPacienteUseCase consultarReservasPacienteUseCase;
    private final CancelarReservaUseCase cancelarReservaUseCase;
    private final RetirarReservaUseCase retirarReservaUseCase;

    public ReservaController(ConsultarReservasPacienteUseCase consultarReservasPacienteUseCase,
                             CancelarReservaUseCase cancelarReservaUseCase,
                             RetirarReservaUseCase retirarReservaUseCase) {
        this.consultarReservasPacienteUseCase = consultarReservasPacienteUseCase;
        this.cancelarReservaUseCase = cancelarReservaUseCase;
        this.retirarReservaUseCase = retirarReservaUseCase;
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<ReservaResponse>> consultarPorPaciente(
            @Parameter(example = "1") @PathVariable Long pacienteId) {
        List<ReservaResponse> reservas = consultarReservasPacienteUseCase.executar(pacienteId).stream()
                .map(ReservaResponse::from)
                .toList();
        return ResponseEntity.ok(reservas);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ReservaResponse.from(cancelarReservaUseCase.executar(id)));
    }

    @PostMapping("/{id}/retirar")
    public ResponseEntity<ReservaResponse> retirar(@Parameter(example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ReservaResponse.from(retirarReservaUseCase.executar(id)));
    }
}
