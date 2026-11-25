package org.democraxssy.dto;

import java.util.List;

/** Vista completa de la partida para un jugador (o publica si yo es null). */
public record Vista(
        String id,
        String fase,
        String anfitrionId,
        int puntosParaGanar,
        List<JugadorPublico> jugadores,
        Ronda ronda,
        Yo yo,
        List<ResumenRonda> historial,
        String ganadorId) {
}
