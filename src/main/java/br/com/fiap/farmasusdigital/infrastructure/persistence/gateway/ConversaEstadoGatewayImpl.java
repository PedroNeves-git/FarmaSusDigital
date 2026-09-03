package br.com.fiap.farmasusdigital.infrastructure.persistence.gateway;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.ConversaEstadoGateway;
import br.com.fiap.farmasusdigital.domain.model.ConversaEstado;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ConversaEstadoJpaEntity;
import br.com.fiap.farmasusdigital.infrastructure.persistence.repository.ConversaEstadoJpaRepository;

@Component
public class ConversaEstadoGatewayImpl implements ConversaEstadoGateway {

    private final ConversaEstadoJpaRepository repository;
    private final Clock clock;

    public ConversaEstadoGatewayImpl(ConversaEstadoJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public ConversaEstado salvar(ConversaEstado conversaEstado) {
        String candidatosSerializados = conversaEstado.getCandidatosMedicamentoIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ConversaEstadoJpaEntity entity = new ConversaEstadoJpaEntity(conversaEstado.getTelefone(),
                conversaEstado.getEstado(), conversaEstado.getReservaRascunhoId(), candidatosSerializados,
                conversaEstado.getCpfTemporario(), conversaEstado.getQuantidadeDesejada(), LocalDateTime.now(clock));
        ConversaEstadoJpaEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    public Optional<ConversaEstado> buscarPorTelefone(String telefone) {
        return repository.findById(telefone).map(this::toDomain);
    }

    @Override
    public void remover(String telefone) {
        repository.deleteById(telefone);
    }

    @Override
    public int removerInativasAntesDe(LocalDateTime limite) {
        return repository.deleteByDataUltimaAtualizacaoBefore(limite);
    }

    private ConversaEstado toDomain(ConversaEstadoJpaEntity entity) {
        List<Long> candidatos = desserializarCandidatos(entity.getCandidatosMedicamentoIds());
        ConversaEstado conversaEstado = new ConversaEstado(entity.getTelefone(), entity.getEstado(),
                entity.getReservaRascunhoId(), candidatos);
        conversaEstado.setCpfTemporario(entity.getCpfTemporario());
        conversaEstado.setQuantidadeDesejada(entity.getQuantidadeDesejada());
        conversaEstado.setDataUltimaAtualizacao(entity.getDataUltimaAtualizacao());
        return conversaEstado;
    }

    private List<Long> desserializarCandidatos(String candidatosSerializados) {
        if (candidatosSerializados == null || candidatosSerializados.isBlank()) {
            return List.of();
        }
        return Arrays.stream(candidatosSerializados.split(","))
                .map(Long::parseLong)
                .toList();
    }
}
