package org.democraxssy.dto;

import java.util.List;

import org.democraxssy.dominio.CartaNegra;

/** Vista de la ronda actual. */
public record Ronda(
        int numero,
        String soberanoId,
        CartaNegra cartaNegra,
        int respuestasRecibidas,
        int votosEmitidos,
        List<RespuestaRevelada> respuestas,
        List<String> favoritasDelPueblo,
        String ganadora) {
}
