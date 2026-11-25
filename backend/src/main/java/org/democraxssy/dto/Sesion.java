package org.democraxssy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Sesion(
        @JsonProperty("partida_id") String partidaId,
        @JsonProperty("jugador_id") String jugadorId,
        String token,
        Vista partida) {
}
