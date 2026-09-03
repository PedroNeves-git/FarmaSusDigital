package br.com.fiap.farmasusdigital.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ReservaJpaEntity;
import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ReservaStatusJpa;

public interface ReservaJpaRepository extends JpaRepository<ReservaJpaEntity, Long> {

    Optional<ReservaJpaEntity> findByPacienteIdAndStatus(Long pacienteId, ReservaStatusJpa status);

    List<ReservaJpaEntity> findByPacienteIdOrderByDataCriacaoDesc(Long pacienteId);

    List<ReservaJpaEntity> findByStatusAndDataExpiracaoBefore(ReservaStatusJpa status, LocalDateTime limite);

    List<ReservaJpaEntity> findByStatusAndLembreteEnviadoFalseAndDataExpiracaoLessThanEqual(
            ReservaStatusJpa status, LocalDateTime limite);
}
