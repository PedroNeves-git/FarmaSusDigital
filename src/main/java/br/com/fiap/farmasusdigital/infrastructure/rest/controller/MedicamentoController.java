package br.com.fiap.farmasusdigital.infrastructure.rest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.farmasusdigital.application.usecase.CadastrarMedicamentoUseCase;
import br.com.fiap.farmasusdigital.application.usecase.ListarMedicamentosUseCase;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.infrastructure.rest.dto.MedicamentoRequest;
import br.com.fiap.farmasusdigital.infrastructure.rest.dto.MedicamentoResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/estoque/medicamentos")
public class MedicamentoController {

    private final ListarMedicamentosUseCase listarMedicamentosUseCase;
    private final CadastrarMedicamentoUseCase cadastrarMedicamentoUseCase;

    public MedicamentoController(ListarMedicamentosUseCase listarMedicamentosUseCase,
                                  CadastrarMedicamentoUseCase cadastrarMedicamentoUseCase) {
        this.listarMedicamentosUseCase = listarMedicamentosUseCase;
        this.cadastrarMedicamentoUseCase = cadastrarMedicamentoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<MedicamentoResponse>> listar() {
        List<MedicamentoResponse> medicamentos = listarMedicamentosUseCase.executar().stream()
                .map(MedicamentoResponse::from)
                .toList();
        return ResponseEntity.ok(medicamentos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentoResponse cadastrar(@Valid @RequestBody MedicamentoRequest request) {
        Medicamento medicamento = cadastrarMedicamentoUseCase.executar(request.nome(), request.quantidadeEstoque());
        return MedicamentoResponse.from(medicamento);
    }
}
