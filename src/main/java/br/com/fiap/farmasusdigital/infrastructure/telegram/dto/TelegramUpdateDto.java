package br.com.fiap.farmasusdigital.infrastructure.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdateDto(@JsonProperty("update_id") Long updateId, TelegramMessageDto message,
                                 @JsonProperty("callback_query") TelegramCallbackQueryDto callbackQuery) {
}
