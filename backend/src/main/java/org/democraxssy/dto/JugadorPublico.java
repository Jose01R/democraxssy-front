package org.democraxssy.dto;

/** Vista publica de un jugador (sin token ni mano). */
public record JugadorPublico(
        String id,
        String nombre,
        int puntos,
        boolean esAnfitrion,
        boolean esSoberano,
        boolean yaRespondio,
        boolean yaVoto) {
}
