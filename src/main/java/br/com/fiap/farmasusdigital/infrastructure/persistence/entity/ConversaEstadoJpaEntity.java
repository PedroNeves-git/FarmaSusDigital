package br.com.fiap.farmasusdigital.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.fiap.farmasusdigital.domain.model.EstadoConversa;

@Entity
@Table(name = "conversa_estado")
public class ConversaEstadoJpaEntity {

    @Id
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoConversa estado;

    @Column(name = "reserva_rascunho_id")
    private Long reservaRascunhoId;

    // ids dos medicamentos candidatos, separados por virgula
    @Column(name = "candidatos_medicamento_ids", length = 500)
    private String candidatosMedicamentoIds;

    @Column(name = "cpf_temporario", length = 11)
    private String cpfTemporario;

    @Column(name = "quantidade_desejada", nullable = false)
    private int quantidadeDesejada = 1;

    @Column(name = "data_ultima_atualizacao", nullable = false)
    private LocalDateTime dataUltimaAtualizacao;

    public ConversaEstadoJpaEntity() {
    }

    public ConversaEstadoJpaEntity(String telefone, EstadoConversa estado, Long reservaRascunhoId,
                                    String candidatosMedicamentoIds, String cpfTemporario, int quantidadeDesejada,
                                    LocalDateTime dataUltimaAtualizacao) {
        this.telefone = telefone;
        this.estado = estado;
        this.reservaRascunhoId = reservaRascunhoId;
        this.candidatosMedicamentoIds = candidatosMedicamentoIds;
        this.cpfTemporario = cpfTemporario;
        this.quantidadeDesejada = quantidadeDesejada;
        this.dataUltimaAtualizacao = dataUltimaAtualizacao;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public EstadoConversa getEstado() {
        return estado;
    }

    public void setEstado(EstadoConversa estado) {
        this.estado = estado;
    }

    public Long getReservaRascunhoId() {
        return reservaRascunhoId;
    }

    public void setReservaRascunhoId(Long reservaRascunhoId) {
        this.reservaRascunhoId = reservaRascunhoId;
    }

    public String getCandidatosMedicamentoIds() {
        return candidatosMedicamentoIds;
    }

    public void setCandidatosMedicamentoIds(String candidatosMedicamentoIds) {
        this.candidatosMedicamentoIds = candidatosMedicamentoIds;
    }

    public String getCpfTemporario() {
        return cpfTemporario;
    }

    public void setCpfTemporario(String cpfTemporario) {
        this.cpfTemporario = cpfTemporario;
    }

    public int getQuantidadeDesejada() {
        return quantidadeDesejada;
    }

    public void setQuantidadeDesejada(int quantidadeDesejada) {
        this.quantidadeDesejada = quantidadeDesejada;
    }

    public LocalDateTime getDataUltimaAtualizacao() {
        return dataUltimaAtualizacao;
    }

    public void setDataUltimaAtualizacao(LocalDateTime dataUltimaAtualizacao) {
        this.dataUltimaAtualizacao = dataUltimaAtualizacao;
    }
}
