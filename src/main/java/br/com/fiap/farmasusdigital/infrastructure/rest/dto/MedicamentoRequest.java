package br.com.fiap.farmasusdigital.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MedicamentoRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @Min(value = 0, message = "quantidadeEstoque não pode ser negativa") int quantidadeEstoque) {
}
