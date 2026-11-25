package org.democraxssy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JugadorPublico(
        String id,
        String nombre,
        int puntos,
        @JsonProperty("es_anfitrion") boolean esAnfitrion,
        @JsonProperty("es_soberano") boolean esSoberano,
        @JsonProperty("ya_respondio") boolean yaRespondio,
        @JsonProperty("ya_voto") boolean yaVoto) {
}
