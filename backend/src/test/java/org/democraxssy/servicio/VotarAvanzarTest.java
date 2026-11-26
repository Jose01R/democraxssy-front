package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.ProhibidoException;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class VotarAvanzarTest {

    private JuegoServicio juego() {
        List<CartaNegra> negras = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            negras.add(new CartaNegra("x ____", 1));
        }
        List<CartaBlanca> blancas = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            blancas.add(new CartaBlanca(i, "c" + i));
        }
        return new JuegoEnMemoria(new RepositorioEnMemoria(), new Mazos(negras, blancas), new Random(1));
    }

    private List<Sesion> iniciar4(JuegoServicio j) {
        Sesion a = j.crearPartida("Ana", 5);
        Sesion b = j.unirse(a.partidaId(), "Beto");
        Sesion c = j.unirse(a.partidaId(), "Caro");
        Sesion d = j.unirse(a.partidaId(), "Dani");
        j.iniciar(a.partidaId(), a.token());
        return List.of(a, b, c, d);
    }

    private int primeraCarta(JuegoServicio j, String pid, String token) {
        return j.obtenerPartida(pid, Optional.of(token)).yo().mano().get(0).id();
    }

    /** Lleva la partida a VOTACION respondiendo con los 3 no-soberanos. */
    private List<Sesion> enVotacion(JuegoServicio j) {
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        for (int i = 1; i <= 3; i++) {
            j.responder(pid, s.get(i).token(), List.of(primeraCarta(j, pid, s.get(i).token())));
        }
        return s;
    }

    private List<String> respuestaIds(JuegoServicio j, String pid) {
        return j.obtenerPartida(pid, Optional.empty()).ronda().respuestas().stream()
                .map(RespuestaRevelada::id).toList();
    }

    @Test
    void votoValidoMarcaYaVotoYMiVoto() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        Sesion beto = s.get(1);
        String rid = respuestaIds(j, pid).get(0);
        Vista v = j.votar(pid, beto.token(), rid);
        assertEquals(rid, v.yo().miVoto());
        assertTrue(v.jugadores().stream()
                .filter(jp -> jp.id().equals(beto.jugadorId())).findFirst().orElseThrow().yaVoto());
    }

    @Test
    void elSoberanoNoVota() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        String rid = respuestaIds(j, pid).get(0);
        assertThrows(ProhibidoException.class, () -> j.votar(pid, s.get(0).token(), rid));
    }

    @Test
    void rechazaVotarDosVeces() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        List<String> ids = respuestaIds(j, pid);
        j.votar(pid, s.get(1).token(), ids.get(0));
        assertThrows(ConflictoException.class, () -> j.votar(pid, s.get(1).token(), ids.get(1)));
    }

    @Test
    void permiteVotarLaPropia() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        Sesion beto = s.get(1);
        String propia = j.obtenerPartida(pid, Optional.of(beto.token())).ronda().respuestas().stream()
                .filter(RespuestaRevelada::esMia).findFirst().orElseThrow().id();
        Vista v = j.votar(pid, beto.token(), propia);
        assertEquals(propia, v.yo().miVoto());
    }

    @Test
    void alVotarTodosPasaADecisionConFavoritasEmpatadas() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        List<String> ids = respuestaIds(j, pid);
        j.votar(pid, s.get(1).token(), ids.get(0));
        j.votar(pid, s.get(2).token(), ids.get(1));
        j.votar(pid, s.get(3).token(), ids.get(2));
        Vista v = j.obtenerPartida(pid, Optional.empty());
        assertEquals("decision", v.fase());
        assertEquals(3, v.ronda().favoritasDelPueblo().size()); // empate a 1 voto
    }

    @Test
    void avanzarEnRespuestasConMenosDe2Rechaza() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        j.responder(pid, s.get(1).token(), List.of(primeraCarta(j, pid, s.get(1).token())));
        assertThrows(ConflictoException.class, () -> j.avanzar(pid, s.get(0).token()));
    }

    @Test
    void avanzarEnRespuestasAbreVotacion() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        j.responder(pid, s.get(1).token(), List.of(primeraCarta(j, pid, s.get(1).token())));
        j.responder(pid, s.get(2).token(), List.of(primeraCarta(j, pid, s.get(2).token())));
        Vista v = j.avanzar(pid, s.get(0).token());
        assertEquals("votacion", v.fase());
    }

    @Test
    void avanzarSoloAnfitrionOSoberano() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        assertThrows(ProhibidoException.class, () -> j.avanzar(pid, s.get(1).token()));
    }

    @Test
    void avanzarEnVotacionCierraADecision() {
        JuegoServicio j = juego();
        List<Sesion> s = enVotacion(j);
        String pid = s.get(0).partidaId();
        Vista v = j.avanzar(pid, s.get(0).token());
        assertEquals("decision", v.fase());
    }
}
