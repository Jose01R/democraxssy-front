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
import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.DatosInvalidosException;
import org.democraxssy.errores.ProhibidoException;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class ResponderTest {

    private JuegoServicio juego() {
        List<CartaNegra> negras = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            negras.add(new CartaNegra("x ____", 1)); // todas elegir 1
        }
        List<CartaBlanca> blancas = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            blancas.add(new CartaBlanca(i, "c" + i));
        }
        return new JuegoEnMemoria(new RepositorioEnMemoria(), new Mazos(negras, blancas), new Random(1));
    }

    /** Crea e inicia una partida de 4; devuelve [anfitrion, b, c, d]. */
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

    @Test
    void respuestaValidaBajaLaManoYMarcaYaRespondio() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        Sesion beto = s.get(1);
        Vista v = j.responder(pid, beto.token(), List.of(primeraCarta(j, pid, beto.token())));
        assertEquals(6, v.yo().mano().size());
        assertTrue(v.jugadores().stream()
                .filter(jp -> jp.id().equals(beto.jugadorId())).findFirst().orElseThrow().yaRespondio());
    }

    @Test
    void elSoberanoNoResponde() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        Sesion anf = s.get(0);
        int carta = primeraCarta(j, anf.partidaId(), anf.token());
        assertThrows(ProhibidoException.class, () -> j.responder(anf.partidaId(), anf.token(), List.of(carta)));
    }

    @Test
    void rechazaCantidadIncorrecta() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        Sesion beto = s.get(1);
        var mano = j.obtenerPartida(pid, Optional.of(beto.token())).yo().mano();
        assertThrows(DatosInvalidosException.class,
                () -> j.responder(pid, beto.token(), List.of(mano.get(0).id(), mano.get(1).id())));
    }

    @Test
    void rechazaResponderDosVeces() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        Sesion beto = s.get(1);
        j.responder(pid, beto.token(), List.of(primeraCarta(j, pid, beto.token())));
        assertThrows(ConflictoException.class,
                () -> j.responder(pid, beto.token(), List.of(primeraCarta(j, pid, beto.token()))));
    }

    @Test
    void alResponderTodosPasaAVotacion() {
        JuegoServicio j = juego();
        List<Sesion> s = iniciar4(j);
        String pid = s.get(0).partidaId();
        for (int i = 1; i <= 3; i++) {
            Sesion jug = s.get(i);
            j.responder(pid, jug.token(), List.of(primeraCarta(j, pid, jug.token())));
        }
        assertEquals("votacion", j.obtenerPartida(pid, Optional.empty()).fase());
    }
}
