package org.democraxssy.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Vista(
        String id,
        String fase,
        @JsonProperty("anfitrion_id") String anfitrionId,
        @JsonProperty("puntos_para_ganar") int puntosParaGanar,
        List<JugadorPublico> jugadores,
        Ronda ronda,
        Yo yo,
        List<ResumenRonda> historial,
        @JsonProperty("ganador_id") String ganadorId) {
}
