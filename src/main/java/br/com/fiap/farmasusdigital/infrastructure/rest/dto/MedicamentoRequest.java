package br.com.fiap.farmasusdigital.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MedicamentoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Schema(example = "Dipirona Sódica 500mg")
        String nome,

        @Min(value = 0, message = "quantidadeEstoque não pode ser negativa")
        @Schema(example = "100")
        int quantidadeEstoque) {
}
