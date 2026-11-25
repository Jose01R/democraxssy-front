package org.democraxssy.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Ronda(
        int numero,
        @JsonProperty("soberano_id") String soberanoId,
        @JsonProperty("carta_negra") CartaNegraDto cartaNegra,
        @JsonProperty("respuestas_recibidas") int respuestasRecibidas,
        @JsonProperty("votos_emitidos") int votosEmitidos,
        List<RespuestaRevelada> respuestas,
        @JsonProperty("favoritas_del_pueblo") List<String> favoritasDelPueblo,
        String ganadora) {
}
