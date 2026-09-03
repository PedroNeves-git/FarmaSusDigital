package br.com.fiap.farmasusdigital.domain.model;

import br.com.fiap.farmasusdigital.domain.exception.EstoqueInsuficienteException;

public class Medicamento {

    private Long id;
    private final String nome;
    private int quantidadeEstoque;

    public Medicamento(Long id, String nome, int quantidadeEstoque) {
        this.id = id;
        this.nome = nome;
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public void reservar(int quantidade) {
        if (quantidade > quantidadeEstoque) {
            throw new EstoqueInsuficienteException(nome);
        }
        quantidadeEstoque -= quantidade;
    }

    public void devolverAoEstoque(int quantidade) {
        quantidadeEstoque += quantidade;
    }

    public boolean temEstoqueDisponivel(int quantidade) {
        return quantidadeEstoque >= quantidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }
}
