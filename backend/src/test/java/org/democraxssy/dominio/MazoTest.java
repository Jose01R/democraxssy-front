package org.democraxssy.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MazoTest {

    @Test
    void reparteHastaVaciarYLuegoReciclaDescartes() {
        Mazo<Integer> m = new Mazo<>(List.of(1, 2, 3), new Random(1));
        Integer a = m.robar();
        Integer b = m.robar();
        Integer c = m.robar();
        m.descartar(List.of(a, b, c));
        // El robo quedo vacio; al robar debe reciclar los descartes y no lanzar.
        assertNotNull(m.robar());
    }

    @Test
    void nuncaDevuelveLaMismaCartaDosVecesSinReciclar() {
        Mazo<Integer> m = new Mazo<>(List.of(1, 2, 3), new Random(1));
        Set<Integer> vistas = new HashSet<>(List.of(m.robar(), m.robar(), m.robar()));
        assertEquals(3, vistas.size());
    }
}
