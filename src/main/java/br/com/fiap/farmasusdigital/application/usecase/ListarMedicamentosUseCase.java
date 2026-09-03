package br.com.fiap.farmasusdigital.application.usecase;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;

@Component
public class ListarMedicamentosUseCase {

    private final MedicamentoGateway medicamentoGateway;

    public ListarMedicamentosUseCase(MedicamentoGateway medicamentoGateway) {
        this.medicamentoGateway = medicamentoGateway;
    }

    public List<Medicamento> executar() {
        return medicamentoGateway.listarTodos();
    }
}
