package br.com.fiap.farmasusdigital.application.usecase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.domain.service.MedicamentoNomeMatcher;

@Component
public class BuscarMedicamentosUseCase {

    private final MedicamentoGateway medicamentoGateway;

    public BuscarMedicamentosUseCase(MedicamentoGateway medicamentoGateway) {
        this.medicamentoGateway = medicamentoGateway;
    }

    public List<Medicamento> executarPorNomeSemelhante(String texto) {
        return medicamentoGateway.listarTodos().stream()
                .filter(medicamento -> MedicamentoNomeMatcher.correspondeA(medicamento.getNome(), texto))
                .toList();
    }

    public Optional<Medicamento> executarPorId(Long id) {
        return medicamentoGateway.buscarPorId(id);
    }
}
