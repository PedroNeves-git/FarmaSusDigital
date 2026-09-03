package br.com.fiap.farmasusdigital.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.farmasusdigital.infrastructure.persistence.entity.PacienteJpaEntity;

public interface PacienteJpaRepository extends JpaRepository<PacienteJpaEntity, Long> {

    Optional<PacienteJpaEntity> findByTelefone(String telefone);

    Optional<PacienteJpaEntity> findByCpf(String cpf);
}
