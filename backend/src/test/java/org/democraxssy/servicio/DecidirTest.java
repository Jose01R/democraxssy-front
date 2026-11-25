package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.democraxssy.errores.ProhibidoException;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class DecidirTest {

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

    private List<Sesion> crearEIniciar(JuegoServicio j, int puntos) {
        Sesion a = j.crearPartida("Ana", puntos);
        Sesion b = j.unirse(a.partidaId(), "Beto");
        Sesion c = j.unirse(a.partidaId(), "Caro");
        Sesion d = j.unirse(a.partidaId(), "Dani");
        j.iniciar(a.partidaId(), a.token());
        return List.of(a, b, c, d);
    }

    private String pid(List<Sesion> s) {
        return s.get(0).partidaId();
    }

    private void responden(JuegoServicio j, List<Sesion> s) {
        for (int i = 1; i <= 3; i++) {
            int carta = j.obtenerPartida(pid(s), Optional.of(s.get(i).token())).yo().mano().get(0).id();
            j.responder(pid(s), s.get(i).token(), List.of(carta));
        }
    }

    private List<String> ids(JuegoServicio j, List<Sesion> s) {
        return j.obtenerPartida(pid(s), Optional.empty()).ronda().respuestas().stream()
                .map(RespuestaRevelada::id).toList();
    }

    private void votanTodos(JuegoServicio j, List<Sesion> s, String respuestaId) {
        for (int i = 1; i <= 3; i++) {
            j.votar(pid(s), s.get(i).token(), respuestaId);
        }
    }

    @Test
    void decidirSumaPuntoYPasaAResultado() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 5);
        responden(j, s);
        List<String> ids = ids(j, s);
        votanTodos(j, s, ids.get(0));
        Vista v = j.decidir(pid(s), s.get(0).token(), ids.get(0));
        assertEquals("resultado", v.fase());
        assertEquals(ids.get(0), v.ronda().ganadora());
        String autor = v.ronda().respuestas().stream()
                .filter(r -> r.id().equals(ids.get(0))).findFirst().orElseThrow().autorId();
        assertEquals(1, v.jugadores().stream()
                .filter(jp -> jp.id().equals(autor)).findFirst().orElseThrow().puntos());
    }

    @Test
    void alcanzarPuntosTermina() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 1);
        responden(j, s);
        List<String> ids = ids(j, s);
        votanTodos(j, s, ids.get(0));
        Vista v = j.decidir(pid(s), s.get(0).token(), ids.get(0));
        assertEquals("terminada", v.fase());
        String autor = v.ronda().respuestas().stream()
                .filter(r -> r.id().equals(ids.get(0))).findFirst().orElseThrow().autorId();
        assertEquals(autor, v.ganadorId());
    }

    @Test
    void respetoTrueCuandoGanadoraEsFavorita() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 5);
        responden(j, s);
        List<String> ids = ids(j, s);
        votanTodos(j, s, ids.get(0));
        Vista v = j.decidir(pid(s), s.get(0).token(), ids.get(0));
        assertTrue(v.historial().get(0).respetoAlPueblo());
    }

    @Test
    void respetoTrueCuandoNadieVoto() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 5);
        responden(j, s);
        j.avanzar(pid(s), s.get(0).token()); // votacion -> decision sin votos
        List<String> ids = ids(j, s);
        Vista v = j.decidir(pid(s), s.get(0).token(), ids.get(0));
        assertTrue(v.historial().get(0).respetoAlPueblo());
    }

    @Test
    void respetoFalseCuandoNoEsLaMasVotada() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 5);
        responden(j, s);
        List<String> ids = ids(j, s);
        votanTodos(j, s, ids.get(0)); // favorita = ids[0]
        Vista v = j.decidir(pid(s), s.get(0).token(), ids.get(1)); // elige otra
        assertFalse(v.historial().get(0).respetoAlPueblo());
    }

    @Test
    void soloElSoberanoDecide() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 5);
        responden(j, s);
        List<String> ids = ids(j, s);
        votanTodos(j, s, ids.get(0));
        assertThrows(ProhibidoException.class, () -> j.decidir(pid(s), s.get(1).token(), ids.get(0)));
    }

    // ---- rotacion del soberano con vuelta ----

    private String soberanoActual(JuegoServicio j, String pid) {
        return j.obtenerPartida(pid, Optional.empty()).ronda().soberanoId();
    }

    private void jugarRonda(JuegoServicio j, List<Sesion> todos) {
        String pid = pid(todos);
        String soberanoId = soberanoActual(j, pid);
        for (Sesion s : todos) {
            if (s.jugadorId().equals(soberanoId)) {
                continue;
            }
            int carta = j.obtenerPartida(pid, Optional.of(s.token())).yo().mano().get(0).id();
            j.responder(pid, s.token(), List.of(carta));
        }
        List<String> rids = j.obtenerPartida(pid, Optional.empty()).ronda().respuestas().stream()
                .map(RespuestaRevelada::id).toList();
        for (Sesion s : todos) {
            if (s.jugadorId().equals(soberanoId)) {
                continue;
            }
            j.votar(pid, s.token(), rids.get(0));
        }
        Sesion soberano = todos.stream()
                .filter(s -> s.jugadorId().equals(soberanoId)).findFirst().orElseThrow();
        j.decidir(pid, soberano.token(), rids.get(0));
        j.avanzar(pid, soberano.token());
    }

    @Test
    void rotacionDelSoberanoConVuelta() {
        JuegoServicio j = juego();
        List<Sesion> s = crearEIniciar(j, 20); // alto para no terminar
        String pid = pid(s);
        List<String> orden = s.stream().map(Sesion::jugadorId).toList();
        assertEquals(orden.get(0), soberanoActual(j, pid)); // anfitrion
        jugarRonda(j, s);
        assertEquals(orden.get(1), soberanoActual(j, pid));
        jugarRonda(j, s);
        assertEquals(orden.get(2), soberanoActual(j, pid));
        jugarRonda(j, s);
        assertEquals(orden.get(3), soberanoActual(j, pid));
        jugarRonda(j, s);
        assertEquals(orden.get(0), soberanoActual(j, pid)); // vuelta al primero
    }
}
