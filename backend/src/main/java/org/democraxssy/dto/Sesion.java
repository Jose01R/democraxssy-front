package org.democraxssy.dto;

/** Respuesta al crear o unirse: credenciales del jugador + vista de la partida. */
public record Sesion(
        String partidaId,
        String jugadorId,
        String token,
        Vista partida) {
}
