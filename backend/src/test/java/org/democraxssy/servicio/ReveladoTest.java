package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.democraxssy.dominio.CartaBlanca;
import org.democraxssy.dominio.CartaNegra;
import org.democraxssy.dominio.Mazos;
import org.democraxssy.dto.RespuestaRevelada;
import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class ReveladoTest {

    private JuegoServicio juego() {
        List<CartaNegra> negras = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            negras.add(new CartaNegra("x ____", 1));
        }
        List<CartaBlanca> blancas = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            blancas.add(new CartaBlanca(i, "c" + i));
        }
        return new JuegoEnMemoria(new RepositorioEnMemoria(), new Mazos(negras, blancas), new Random(1));
    }

    private List<Sesion> crearEIniciar(JuegoServicio j) {
        Sesion a = j.crearPartida("Ana", 5);
        Sesion b = j.unirse(a.partidaId(), "Beto");
        Sesion c = j.unirse(a.partidaId(), "Caro");
        Sesion d = j.unirse(a.partidaId(), "Dani");
        j.iniciar(a.partidaId(), a.token());
        return List.of(a, b, c, d);
    }

    private String pid(List<Sesion> s) {
        return s.get(0).partidaId();
    }

    private int primeraCarta(JuegoServicio j, String pid, String token) {
        return j.obtenerPartida(pid, Optional.of(token)).yo().mano().get(0).id();
    }

    private void responden(JuegoServicio j, List<Sesion> s) {
        for (int i = 1; i <= 3; i++) {
            j.responder(pid(s), s.get(i).token(), List.of(primeraCarta(j, pid(s), s.get(i).token())));
        }
    }

    private List<String> ids(JuegoServicio j, List<Sesion> s) {
        return j.obtenerPartida(pid(s), Optional.empty()).ronda().respuestas().stream()
                .map(RespuestaRevelada::id).toList();
    }

    @Test
    void enRespuestasLasRespuestasEstanOcultas() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j);
        j.responder(pid(s), s.get(1).token(), List.of(primeraCarta(j, pid(s), s.get(1).token())));
        Vista v = j.obtenerPartida(pid(s), Optional.empty());
        assertEquals("respuestas", v.fase());
        assertTrue(v.ronda().respuestas().isEmpty());
    }

    @Test
    void enVotacionSonAnonimasYSinVotos() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j);
        responden(j, s);
        Vista v = j.obtenerPartida(pid(s), Optional.empty());
        assertEquals("votacion", v.fase());
        assertFalse(v.ronda().respuestas().isEmpty());
        for (RespuestaRevelada r : v.ronda().respuestas()) {
            assertNull(r.autorId(), "no se revela el autor antes de resultado");
            assertNull(r.votos(), "no se revelan los votos antes de decision");
        }
    }

    @Test
    void enDecisionAparecenVotosPeroNoAutor() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j);
        responden(j, s);
        for (int i = 1; i <= 3; i++) {
            j.votar(pid(s), s.get(i).token(), ids(j, s).get(0));
        }
        Vista v = j.obtenerPartida(pid(s), Optional.empty());
        assertEquals("decision", v.fase());
        for (RespuestaRevelada r : v.ronda().respuestas()) {
            assertNull(r.autorId());
        }
        assertTrue(v.ronda().respuestas().stream().anyMatch(r -> r.votos() != null && r.votos() > 0));
    }

    @Test
    void enResultadoApareceElAutor() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j);
        responden(j, s);
        List<String> ids = ids(j, s);
        for (int i = 1; i <= 3; i++) {
            j.votar(pid(s), s.get(i).token(), ids.get(0));
        }
        j.decidir(pid(s), s.get(0).token(), ids.get(0));
        Vista v = j.obtenerPartida(pid(s), Optional.empty());
        assertEquals("resultado", v.fase());
        for (RespuestaRevelada r : v.ronda().respuestas()) {
            assertNotNull(r.autorId(), "el autor se revela desde resultado");
        }
    }

    @Test
    void cadaJugadorSoloVeSuPropiaMano() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j);
        responden(j, s);
        Sesion beto = s.get(1);
        Vista vBeto = j.obtenerPartida(pid(s), Optional.of(beto.token()));
        assertEquals(beto.jugadorId(), vBeto.yo().id());
        assertEquals(1, vBeto.ronda().respuestas().stream().filter(RespuestaRevelada::esMia).count());
        Vista publica = j.obtenerPartida(pid(s), Optional.empty());
        assertNull(publica.yo(), "la vista publica no trae datos privados");
    }
}
