package org.democraxssy.dominio;

/** Entrada del historial de una ronda ya resuelta (datos de dominio). */
public record ResultadoRonda(
        int numero,
        String soberanoId,
        String ganadorId,
        String frase,
        boolean respetoAlPueblo) {
}
