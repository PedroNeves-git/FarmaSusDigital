package br.com.fiap.farmasusdigital.infrastructure.rest.dto;

import br.com.fiap.farmasusdigital.domain.model.Medicamento;

public record MedicamentoResponse(Long id, String nome, int quantidadeEstoque) {

    public static MedicamentoResponse from(Medicamento medicamento) {
        return new MedicamentoResponse(medicamento.getId(), medicamento.getNome(), medicamento.getQuantidadeEstoque());
    }
}
