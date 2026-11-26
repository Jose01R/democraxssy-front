package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.democraxssy.dominio.CartaBlanca;
import org.democraxssy.dominio.CartaNegra;
import org.democraxssy.dominio.Mazos;
import org.democraxssy.dto.JugadorPublico;
import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.ProhibidoException;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class IniciarTest {

    private JuegoServicio juego() {
        List<CartaNegra> negras = List.of(new CartaNegra("a ____", 1), new CartaNegra("b ____ ____", 2));
        List<CartaBlanca> blancas = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            blancas.add(new CartaBlanca(i, "c" + i));
        }
        return new JuegoEnMemoria(new RepositorioEnMemoria(), new Mazos(negras, blancas), new Random(1));
    }

    private Sesion partidaDe(JuegoServicio j, int jugadores) {
        Sesion anfitrion = j.crearPartida("Ana", 5);
        for (int i = 1; i < jugadores; i++) {
            j.unirse(anfitrion.partidaId(), "N" + i);
        }
        return anfitrion;
    }

    @Test
    void iniciarReparte7YPoneSoberanoAlAnfitrion() {
        JuegoServicio j = juego();
        Sesion anfitrion = partidaDe(j, 4);
        Vista v = j.iniciar(anfitrion.partidaId(), anfitrion.token());
        assertEquals("respuestas", v.fase());
        assertEquals(1, v.ronda().numero());
        assertEquals(anfitrion.jugadorId(), v.ronda().soberanoId());
        assertEquals(7, v.yo().mano().size());
        assertEquals(1, v.jugadores().stream().filter(JugadorPublico::esSoberano).count());
    }

    @Test
    void rechazaNumeroImparDeJugadores() {
        JuegoServicio j = juego();
        Sesion a = partidaDe(j, 5); // impar, >= 4
        assertThrows(ConflictoException.class, () -> j.iniciar(a.partidaId(), a.token()));
    }

    @Test
    void rechazaMenosDeCuatro() {
        JuegoServicio j = juego();
        Sesion a = partidaDe(j, 2);
        assertThrows(ConflictoException.class, () -> j.iniciar(a.partidaId(), a.token()));
    }

    @Test
    void rechazaSiNoEsAnfitrion() {
        JuegoServicio j = juego();
        Sesion a = j.crearPartida("Ana", 5);
        Sesion b = j.unirse(a.partidaId(), "Beto");
        j.unirse(a.partidaId(), "Caro");
        j.unirse(a.partidaId(), "Dani");
        assertThrows(ProhibidoException.class, () -> j.iniciar(a.partidaId(), b.token()));
    }
}
