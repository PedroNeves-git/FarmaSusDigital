package br.com.fiap.farmasusdigital.infrastructure.rest.dto;

import br.com.fiap.farmasusdigital.domain.model.ItemReserva;

public record ItemReservaResponse(Long medicamentoId, String nomeMedicamento, int quantidade) {

    public static ItemReservaResponse from(ItemReserva item) {
        return new ItemReservaResponse(item.getMedicamentoId(), item.getNomeMedicamento(), item.getQuantidade());
    }
}
