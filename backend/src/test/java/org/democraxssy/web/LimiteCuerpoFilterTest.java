package org.democraxssy.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LimiteCuerpoFilterTest {

    private final LimiteCuerpoFilter filtro = new LimiteCuerpoFilter();

    private MockHttpServletRequest peticion(byte[] cuerpo) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api/partidas");
        peticion.setContent(cuerpo);
        peticion.setContentType("application/json");
        return peticion;
    }

    @Test
    void rechazaCuerpoMayorA4KbCon413() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion(new byte[LimiteCuerpoFilter.MAX_BYTES + 1]), respuesta, cadena);

        assertEquals(413, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("demasiado grande"));
        assertTrue(respuesta.getContentType().startsWith("application/json"));
        assertNull(cadena.getRequest(), "no debe pasar al resto de la cadena");
    }

    @Test
    void dejaPasarCuerpoPequeno() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion("{\"nombre\":\"Ana\"}".getBytes(StandardCharsets.UTF_8)), respuesta, cadena);

        assertNotEquals(413, respuesta.getStatus());
        assertEquals("{\"nombre\":\"Ana\"}", leerCuerpo(cadena));
    }

    @Test
    void elLimiteExactoDe4KbPasa() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion(new byte[LimiteCuerpoFilter.MAX_BYTES]), respuesta, cadena);

        assertNotEquals(413, respuesta.getStatus());
        assertEquals(LimiteCuerpoFilter.MAX_BYTES, cadena.getRequest().getContentLength());
    }

    private static String leerCuerpo(MockFilterChain cadena) throws Exception {
        return new String(cadena.getRequest().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
