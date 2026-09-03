package br.com.fiap.farmasusdigital.infrastructure.persistence.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.ConversaEstadoJpaEntity;

public interface ConversaEstadoJpaRepository extends JpaRepository<ConversaEstadoJpaEntity, String> {

    @Modifying
    @Transactional
    int deleteByDataUltimaAtualizacaoBefore(LocalDateTime limite);
}
