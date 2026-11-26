package org.democraxssy.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.democraxssy.dominio.Partida;
import org.democraxssy.errores.NoEncontradoException;
import org.junit.jupiter.api.Test;

class RepositorioEnMemoriaTest {

    private final RepositorioEnMemoria repo = new RepositorioEnMemoria();

    private Partida guardarPartida(String id) {
        Partida p = new Partida(id, "anfitrion", 5);
        repo.guardar(p);
        return p;
    }

    @Test
    void obtenerRenuevaLaUltimaActividad() {
        Partida p = guardarPartida("p1");
        Instant viejo = Instant.now().minus(1, ChronoUnit.HOURS);
        p.ultimaActividad(viejo);

        repo.obtener("p1");

        assertTrue(p.ultimaActividad().isAfter(viejo),
                "obtener deberia renovar la ultima actividad");
    }

    @Test
    void eliminarInactivasBorraSoloLasAnterioresAlLimite() {
        Partida vieja = guardarPartida("vieja");
        vieja.ultimaActividad(Instant.now().minus(40, ChronoUnit.MINUTES));
        Partida fresca = guardarPartida("fresca");
        fresca.ultimaActividad(Instant.now());

        int borradas = repo.eliminarInactivas(Instant.now().minus(30, ChronoUnit.MINUTES));

        assertEquals(1, borradas);
        assertThrows(NoEncontradoException.class, () -> repo.obtener("vieja"));
        repo.obtener("fresca"); // sigue existiendo (no lanza)
    }
}
