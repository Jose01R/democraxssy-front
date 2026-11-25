package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.democraxssy.dominio.CartaBlanca;
import org.democraxssy.dominio.CartaNegra;
import org.democraxssy.dominio.Mazos;
import org.democraxssy.dto.JugadorPublico;
import org.democraxssy.dto.RespuestaRevelada;
import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class PartidaCompletaTest {

    /** Mazo chico (30 blancas para 4x7=28) a proposito: fuerza el reciclaje de descartes. */
    private JuegoServicio juego() {
        List<CartaNegra> negras = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            negras.add(new CartaNegra("x ____", 1));
        }
        List<CartaBlanca> blancas = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            blancas.add(new CartaBlanca(i, "c" + i));
        }
        return new JuegoEnMemoria(new RepositorioEnMemoria(), new Mazos(negras, blancas), new Random(7));
    }

    @Test
    void partidaDe4DePrincipioAFinTerminaPorPuntos() {
        JuegoServicio j = juego();
        Sesion a = j.crearPartida("Ana", 2); // 2 puntos para ganar
        Sesion b = j.unirse(a.partidaId(), "Beto");
        Sesion c = j.unirse(a.partidaId(), "Caro");
        Sesion d = j.unirse(a.partidaId(), "Dani");
        String pid = a.partidaId();
        j.iniciar(pid, a.token());
        List<Sesion> todos = List.of(a, b, c, d);

        int guardia = 0;
        Vista v = j.obtenerPartida(pid, Optional.empty());
        while (!v.fase().equals("terminada") && guardia++ < 100) {
            String soberanoId = v.ronda().soberanoId();
            // los no-soberanos responden
            for (Sesion s : todos) {
                if (s.jugadorId().equals(soberanoId)) {
                    continue;
                }
                int carta = j.obtenerPartida(pid, Optional.of(s.token())).yo().mano().get(0).id();
                j.responder(pid, s.token(), List.of(carta));
            }
            // votan por la primera respuesta
            List<String> ids = j.obtenerPartida(pid, Optional.empty()).ronda().respuestas().stream()
                    .map(RespuestaRevelada::id).toList();
            for (Sesion s : todos) {
                if (s.jugadorId().equals(soberanoId)) {
                    continue;
                }
                j.votar(pid, s.token(), ids.get(0));
            }
            // el soberano decide y, si sigue, avanza a la siguiente ronda
            Sesion soberano = todos.stream()
                    .filter(s -> s.jugadorId().equals(soberanoId)).findFirst().orElseThrow();
            Vista tras = j.decidir(pid, soberano.token(), ids.get(0));
            if (!tras.fase().equals("terminada")) {
                j.avanzar(pid, soberano.token());
            }
            v = j.obtenerPartida(pid, Optional.empty());
        }

        Vista fin = v;
        assertEquals("terminada", fin.fase());
        assertNotNull(fin.ganadorId());
        int puntosGanador = fin.jugadores().stream()
                .filter(jp -> jp.id().equals(fin.ganadorId())).findFirst().orElseThrow().puntos();
        assertTrue(puntosGanador >= 2, "el ganador alcanzo los puntos");
        // cada ronda suma exactamente 1 punto en total
        int totalPuntos = fin.jugadores().stream().mapToInt(JugadorPublico::puntos).sum();
        assertEquals(fin.historial().size(), totalPuntos);
    }
}
