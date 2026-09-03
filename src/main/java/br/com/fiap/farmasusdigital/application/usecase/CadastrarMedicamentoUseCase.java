package br.com.fiap.farmasusdigital.application.usecase;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;

@Component
public class CadastrarMedicamentoUseCase {

    private final MedicamentoGateway medicamentoGateway;

    public CadastrarMedicamentoUseCase(MedicamentoGateway medicamentoGateway) {
        this.medicamentoGateway = medicamentoGateway;
    }

    public Medicamento executar(String nome, int quantidadeEstoque) {
        Medicamento medicamento = new Medicamento(null, nome.trim(), quantidadeEstoque);
        return medicamentoGateway.salvar(medicamento);
    }
}
