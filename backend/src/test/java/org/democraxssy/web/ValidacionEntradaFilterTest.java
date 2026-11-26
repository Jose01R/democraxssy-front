package org.democraxssy.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ValidacionEntradaFilterTest {

    private final ValidacionEntradaFilter filtro = new ValidacionEntradaFilter();

    private void correr(MockHttpServletRequest peticion, MockHttpServletResponse respuesta,
            MockFilterChain cadena) throws Exception {
        filtro.doFilter(peticion, respuesta, cadena);
    }

    @Test
    void cabeceraDeOverrideDa409() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api/partidas");
        peticion.addHeader("X-HTTP-Method-Override", "DELETE");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertEquals(409, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("error"));
        assertNull(cadena.getRequest(), "no debe pasar al resto de la cadena");
    }

    @Test
    void getConContentTypeDa409() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/partidas/x");
        peticion.setContentType("application/json");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertEquals(409, respuesta.getStatus());
        assertNull(cadena.getRequest());
    }

    @Test
    void getConCuerpoDa409() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/partidas/x");
        peticion.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertEquals(409, respuesta.getStatus());
        assertNull(cadena.getRequest());
    }

    @Test
    void parametroDeConsultaDa409() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/partidas/x");
        peticion.setQueryString("x=1");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertEquals(409, respuesta.getStatus());
        assertNull(cadena.getRequest(), "ninguna ruta admite parametros de consulta");
    }

    @Test
    void getLimpioPasa() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/partidas/x");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertNotNull(cadena.getRequest(), "un GET limpio debe seguir la cadena");
    }

    @Test
    void headSaludLimpioPasa() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("HEAD", "/api/salud");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertNotNull(cadena.getRequest());
    }

    @Test
    void postConContentTypePasa() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api/partidas");
        peticion.setContentType("application/json");
        peticion.setContent("{\"nombre\":\"Ana\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        correr(peticion, respuesta, cadena);

        assertNotNull(cadena.getRequest(), "un POST con cuerpo es normal");
    }
}
