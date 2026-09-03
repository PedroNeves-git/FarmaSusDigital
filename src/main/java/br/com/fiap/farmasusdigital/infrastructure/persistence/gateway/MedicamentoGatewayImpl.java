package br.com.fiap.farmasusdigital.infrastructure.persistence.gateway;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.MedicamentoJpaEntity;
import br.com.fiap.farmasusdigital.infrastructure.persistence.repository.MedicamentoJpaRepository;

@Component
public class MedicamentoGatewayImpl implements MedicamentoGateway {

    private final MedicamentoJpaRepository repository;

    public MedicamentoGatewayImpl(MedicamentoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Medicamento salvar(Medicamento medicamento) {
        MedicamentoJpaEntity entity = new MedicamentoJpaEntity(medicamento.getId(), medicamento.getNome(),
                medicamento.getQuantidadeEstoque());
        MedicamentoJpaEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    public List<Medicamento> listarTodos() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Medicamento> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    private Medicamento toDomain(MedicamentoJpaEntity entity) {
        return new Medicamento(entity.getId(), entity.getNome(), entity.getQuantidadeEstoque());
    }
}
