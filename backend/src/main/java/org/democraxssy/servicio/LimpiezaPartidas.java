package org.democraxssy.servicio;

import java.time.Duration;
import java.time.Instant;

import org.democraxssy.repositorio.RepositorioPartidas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido periodico que elimina partidas abandonadas para no agotar la memoria (-Xmx256m).
 *
 * <p>Una partida se considera abandonada si nadie la toca durante {@code juego.limpieza.timeout}.
 * Como toda operacion (incluido el polling del frontend) pasa por el repositorio y renueva la
 * marca de actividad, solo expiran las salas que de verdad quedaron sin nadie mirandolas.
 * El barrido corre cada {@code juego.limpieza.intervalo}.
 */
@Component
public class LimpiezaPartidas {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaPartidas.class);

    private final RepositorioPartidas repositorio;
    private final Duration timeout;

    public LimpiezaPartidas(RepositorioPartidas repositorio,
            @Value("${juego.limpieza.timeout}") Duration timeout) {
        this.repositorio = repositorio;
        this.timeout = timeout;
    }

    @Scheduled(fixedDelayString = "${juego.limpieza.intervalo}")
    public void limpiar() {
        int borradas = repositorio.eliminarInactivas(Instant.now().minus(timeout));
        if (borradas > 0) {
            log.info("Limpieza: {} partida(s) inactiva(s) eliminada(s)", borradas);
        }
    }
}
