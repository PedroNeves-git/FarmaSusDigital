package br.com.fiap.farmasusdigital.infrastructure.persistence.gateway;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.PacienteGateway;
import br.com.fiap.farmasusdigital.domain.model.Paciente;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.PacienteJpaEntity;
import br.com.fiap.farmasusdigital.infrastructure.persistence.repository.PacienteJpaRepository;

@Component
public class PacienteGatewayImpl implements PacienteGateway {

    private final PacienteJpaRepository repository;

    public PacienteGatewayImpl(PacienteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Paciente salvar(Paciente paciente) {
        PacienteJpaEntity entity = new PacienteJpaEntity(paciente.getId(), paciente.getCpf(),
                paciente.getNomeCompleto(), paciente.getTelefone());
        PacienteJpaEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    public Optional<Paciente> buscarPorTelefone(String telefone) {
        return repository.findByTelefone(telefone).map(this::toDomain);
    }

    @Override
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return repository.findByCpf(cpf).map(this::toDomain);
    }

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    private Paciente toDomain(PacienteJpaEntity entity) {
        return new Paciente(entity.getId(), entity.getCpf(), entity.getNomeCompleto(), entity.getTelefone());
    }
}
