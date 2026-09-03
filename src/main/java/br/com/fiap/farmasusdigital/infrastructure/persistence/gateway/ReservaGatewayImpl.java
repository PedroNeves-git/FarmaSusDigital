package br.com.fiap.farmasusdigital.infrastructure.persistence.gateway;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Reserva;
import br.com.fiap.farmasusdigital.domain.model.ReservaStatus;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ReservaItemJpaEntity;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ReservaJpaEntity;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ReservaStatusJpa;
import br.com.fiap.farmasusdigital.infrastructure.persistence.repository.ReservaJpaRepository;

@Component
public class ReservaGatewayImpl implements ReservaGateway {

    private final ReservaJpaRepository repository;

    public ReservaGatewayImpl(ReservaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Reserva salvar(Reserva reserva) {
        ReservaJpaEntity entity = reserva.getId() != null
                ? repository.findById(reserva.getId()).orElseGet(ReservaJpaEntity::new)
                : new ReservaJpaEntity();

        entity.setPacienteId(reserva.getPacienteId());
        entity.setStatus(toJpaStatus(reserva.getStatus()));
        entity.setDataCriacao(reserva.getDataCriacao());
        entity.setDataExpiracao(reserva.getDataExpiracao());
        entity.setDataFinalizacao(reserva.getDataFinalizacao());
        entity.setLembreteEnviado(reserva.isLembreteEnviado());

        entity.getItens().clear();
        for (ItemReserva item : reserva.getItens()) {
            entity.getItens().add(new ReservaItemJpaEntity(item.getId(), entity, item.getMedicamentoId(),
                    item.getNomeMedicamento(), item.getQuantidade()));
        }

        ReservaJpaEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    public Optional<Reserva> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Reserva> buscarAtivaPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdAndStatus(pacienteId, ReservaStatusJpa.ATIVA).map(this::toDomain);
    }

    @Override
    public List<Reserva> buscarPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdOrderByDataCriacaoDesc(pacienteId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reserva> buscarAtivasVencidas(LocalDateTime agora) {
        return repository.findByStatusAndDataExpiracaoBefore(ReservaStatusJpa.ATIVA, agora).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<Reserva> buscarAtivasSemLembreteVencendoAte(LocalDateTime limite) {
        return repository.findByStatusAndLembreteEnviadoFalseAndDataExpiracaoLessThanEqual(ReservaStatusJpa.ATIVA, limite)
                .stream().map(this::toDomain).toList();
    }

    private Reserva toDomain(ReservaJpaEntity entity) {
        List<ItemReserva> itens = new ArrayList<>();
        for (ReservaItemJpaEntity item : entity.getItens()) {
            itens.add(new ItemReserva(item.getId(), item.getMedicamentoId(), item.getNomeMedicamento(), item.getQuantidade()));
        }
        return new Reserva(entity.getId(), entity.getPacienteId(), itens, toDomainStatus(entity.getStatus()),
                entity.getDataCriacao(), entity.getDataExpiracao(), entity.getDataFinalizacao(),
                entity.isLembreteEnviado());
    }

    private ReservaStatusJpa toJpaStatus(ReservaStatus status) {
        return ReservaStatusJpa.valueOf(status.name());
    }

    private ReservaStatus toDomainStatus(ReservaStatusJpa status) {
        return ReservaStatus.valueOf(status.name());
    }
}
