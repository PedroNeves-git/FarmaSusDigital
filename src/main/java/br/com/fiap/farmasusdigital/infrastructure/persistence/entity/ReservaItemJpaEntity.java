package br.com.fiap.farmasusdigital.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva_item")
public class ReservaItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaJpaEntity reserva;

    @Column(name = "medicamento_id", nullable = false)
    private Long medicamentoId;

    @Column(name = "nome_medicamento", nullable = false)
    private String nomeMedicamento;

    @Column(nullable = false)
    private int quantidade;

    public ReservaItemJpaEntity() {
    }

    public ReservaItemJpaEntity(Long id, ReservaJpaEntity reserva, Long medicamentoId, String nomeMedicamento, int quantidade) {
        this.id = id;
        this.reserva = reserva;
        this.medicamentoId = medicamentoId;
        this.nomeMedicamento = nomeMedicamento;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReservaJpaEntity getReserva() {
        return reserva;
    }

    public void setReserva(ReservaJpaEntity reserva) {
        this.reserva = reserva;
    }

    public Long getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(Long medicamentoId) {
        this.medicamentoId = medicamentoId;
    }

    public String getNomeMedicamento() {
        return nomeMedicamento;
    }

    public void setNomeMedicamento(String nomeMedicamento) {
        this.nomeMedicamento = nomeMedicamento;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
