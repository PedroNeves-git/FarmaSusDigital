package br.com.fiap.farmasusdigital.infrastructure.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramChatDto(Long id) {
}
