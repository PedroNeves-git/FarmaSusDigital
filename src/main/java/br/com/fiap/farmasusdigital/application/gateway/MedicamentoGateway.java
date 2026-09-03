package br.com.fiap.farmasusdigital.application.gateway;

import java.util.List;
import java.util.Optional;

import br.com.fiap.farmasusdigital.domain.model.Medicamento;

public interface MedicamentoGateway {

    Medicamento salvar(Medicamento medicamento);

    List<Medicamento> listarTodos();

    Optional<Medicamento> buscarPorId(Long id);
}
