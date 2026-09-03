package br.com.fiap.farmasusdigital.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.MedicamentoJpaEntity;

public interface MedicamentoJpaRepository extends JpaRepository<MedicamentoJpaEntity, Long> {
}
