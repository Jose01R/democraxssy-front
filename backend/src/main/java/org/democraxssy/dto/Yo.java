package org.democraxssy.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Yo(
        String id,
        String nombre,
        List<CartaBlancaDto> mano,
        @JsonProperty("mi_voto") String miVoto,
        @JsonProperty("puede_avanzar") boolean puedeAvanzar) {
}
