package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CargadorMazoTest {

    @Test
    void cargaUnMazoValido() {
        var m = CargadorMazo.desdeJson("""
            {"negras":[{"id":0,"texto":"a ____","elegir":1}],
             "blancas":[{"id":0,"texto":"x"}]}""");
        assertEquals(1, m.negras().size());
        assertEquals(1, m.blancas().size());
    }

    @Test
    void rechazaNegraConHuecosDistintosDeElegir() {
        assertThrows(MazoInvalidoException.class, () -> CargadorMazo.desdeJson("""
            {"negras":[{"id":0,"texto":"a ____","elegir":2}],
             "blancas":[{"id":0,"texto":"x"}]}"""));
    }

    @Test
    void rechazaElegirFueraDeRango() {
        assertThrows(MazoInvalidoException.class, () -> CargadorMazo.desdeJson("""
            {"negras":[{"id":0,"texto":"a ____ ____ ____","elegir":3}],
             "blancas":[{"id":0,"texto":"x"}]}"""));
    }

    @Test
    void rechazaBlancaConTextoVacio() {
        assertThrows(MazoInvalidoException.class, () -> CargadorMazo.desdeJson("""
            {"negras":[{"id":0,"texto":"a ____","elegir":1}],
             "blancas":[{"id":0,"texto":"  "}]}"""));
    }
}
