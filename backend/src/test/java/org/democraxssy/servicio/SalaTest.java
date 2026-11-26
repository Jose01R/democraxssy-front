package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.democraxssy.dominio.Mazos;
import org.democraxssy.dto.Sesion;
import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.DatosInvalidosException;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class SalaTest {

    private JuegoServicio juego() {
        Mazos m = CargadorMazo.desdeJson("""
            {"negras":[{"id":0,"texto":"a ____","elegir":1}],
             "blancas":[{"id":0,"texto":"x"},{"id":1,"texto":"y"}]}""");
        return new JuegoEnMemoria(new RepositorioEnMemoria(), m, new Random(1));
    }

    @Test
    void crearDejaAlAnfitrionEnSalaConDefault5() {
        Sesion s = juego().crearPartida("Ana", null);
        assertEquals("sala", s.partida().fase());
        assertEquals(5, s.partida().puntosParaGanar());
        assertEquals(1, s.partida().jugadores().size());
        assertTrue(s.token().matches("^[0-9a-f]{64}$"));
        assertTrue(s.partida().jugadores().get(0).esAnfitrion());
    }

    @Test
    void unirseAgregaJugadorYDaOtroToken() {
        JuegoServicio j = juego();
        Sesion a = j.crearPartida("Ana", 7);
        Sesion b = j.unirse(a.partidaId(), "Beto");
        assertEquals(2, b.partida().jugadores().size());
        assertNotEquals(a.token(), b.token());
        assertEquals(7, b.partida().puntosParaGanar());
    }

    @Test
    void rechazaNombreDuplicadoPorMayusculasYEspacios() {
        JuegoServicio j = juego();
        Sesion a = j.crearPartida("Ana", null);
        assertThrows(ConflictoException.class, () -> j.unirse(a.partidaId(), "  ana "));
    }

    @Test
    void rechazaUnirseConPartidaLlena() {
        JuegoServicio j = juego();
        Sesion a = j.crearPartida("A0", null);
        for (int i = 1; i < 22; i++) {
            j.unirse(a.partidaId(), "N" + i);
        }
        assertThrows(ConflictoException.class, () -> j.unirse(a.partidaId(), "N22"));
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(DatosInvalidosException.class, () -> juego().crearPartida("   ", null));
    }
}
