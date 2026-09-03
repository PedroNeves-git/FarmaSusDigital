package br.com.fiap.farmasusdigital.infrastructure.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.fiap.farmasusdigital.domain.model.Reserva;
import br.com.fiap.farmasusdigital.domain.model.ReservaStatus;

public record ReservaResponse(Long id, Long pacienteId, ReservaStatus status, LocalDateTime dataCriacao,
                               LocalDateTime dataExpiracao, LocalDateTime dataFinalizacao,
                               List<ItemReservaResponse> itens) {

    public static ReservaResponse from(Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getPacienteId(), reserva.getStatus(),
                reserva.getDataCriacao(), reserva.getDataExpiracao(), reserva.getDataFinalizacao(),
                reserva.getItens().stream().map(ItemReservaResponse::from).toList());
    }
}
