package br.com.fiap.farmasusdigital.infrastructure.telegram.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramGetUpdatesResponseDto(boolean ok, List<TelegramUpdateDto> result) {
}
