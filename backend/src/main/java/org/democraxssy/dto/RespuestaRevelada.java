package org.democraxssy.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * votos (desde decision) y autor_id (desde resultado) se omiten del JSON cuando son null:
 * el contrato los define como ausentes antes de esas fases, no como null.
 */
public record RespuestaRevelada(
        String id,
        List<String> cartas,
        String frase,
        @JsonProperty("es_mia") boolean esMia,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer votos,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("autor_id") String autorId) {
}
