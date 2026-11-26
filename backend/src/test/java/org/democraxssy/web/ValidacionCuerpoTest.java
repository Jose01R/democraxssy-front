package org.democraxssy.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.democraxssy.config.JacksonConfig;
import org.democraxssy.controllers.PartidasController;
import org.democraxssy.servicio.JuegoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contrato trata un cuerpo con un campo desconocido o con un tipo incorrecto como
 * inesperado (Conflicto) → 409, no como algo que se acepta en silencio.
 */
@WebMvcTest(PartidasController.class)
@Import(JacksonConfig.class)
class ValidacionCuerpoTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JuegoServicio juego;

    private void enviar(String cuerpo, int esperado) throws Exception {
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is(esperado));
    }

    @Test
    void campoDesconocidoDa409() throws Exception {
        enviar("{\"nombre\":\"Ana\",\"admin\":true}", 409);
    }

    @Test
    void tipoIncorrectoEnNombreDa409() throws Exception {
        enviar("{\"nombre\":5}", 409);
    }

    @Test
    void cuerpoValidoNoSeRechaza() throws Exception {
        enviar("{\"nombre\":\"Ana\",\"puntos_para_ganar\":5}", 201);
    }

    @Test
    void puntosAusenteNoSeRechaza() throws Exception {
        // puntos_para_ganar es opcional (el servicio usa 5 por defecto): no debe dar 409.
        enviar("{\"nombre\":\"Ana\"}", 201);
    }

    @Test
    void puntosComoTextoDa409() throws Exception {
        enviar("{\"nombre\":\"Ana\",\"puntos_para_ganar\":\"5\"}", 409);
    }

    @Test
    void puntosDecimalDa409() throws Exception {
        enviar("{\"nombre\":\"Ana\",\"puntos_para_ganar\":5.5}", 409);
    }
}
