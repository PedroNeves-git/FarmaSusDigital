package br.com.fiap.farmasusdigital.domain.model;

public class ItemReserva {

    private Long id;
    private final Long medicamentoId;
    private final String nomeMedicamento;
    private final int quantidade;

    public ItemReserva(Long id, Long medicamentoId, String nomeMedicamento, int quantidade) {
        this.id = id;
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

    public Long getMedicamentoId() {
        return medicamentoId;
    }

    public String getNomeMedicamento() {
        return nomeMedicamento;
    }

    public int getQuantidade() {
        return quantidade;
    }
}
