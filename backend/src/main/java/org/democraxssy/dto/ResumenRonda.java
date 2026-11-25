package org.democraxssy.dto;

/** Entrada del historial: como quedo una ronda. */
public record ResumenRonda(
        int numero,
        String soberanoId,
        String ganadorId,
        String frase,
        boolean respetoAlPueblo) {
}
