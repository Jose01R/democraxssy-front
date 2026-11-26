package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void generaIdsConElLargoYFormatoCorrectos() {
        assertTrue(Ids.partida().matches("^[0-9a-f]{32}$"), "partida = 32 hex");
        assertTrue(Ids.jugador().matches("^[0-9a-f]{16}$"), "jugador = 16 hex");
        assertTrue(Ids.token().matches("^[0-9a-f]{64}$"), "token = 64 hex");
        assertTrue(Ids.respuesta().matches("^[0-9a-f]{12}$"), "respuesta = 12 hex");
    }

    @Test
    void losTokensSonAleatorios() {
        assertNotEquals(Ids.token(), Ids.token());
    }
}
