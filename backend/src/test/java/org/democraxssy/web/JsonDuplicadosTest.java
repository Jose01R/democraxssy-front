package org.democraxssy.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
 * El contrato trata un cuerpo JSON con claves repetidas como una solicitud inesperada
 * (Conflicto) → 409, no como un simple 400 de cuerpo mal formado.
 */
@WebMvcTest(PartidasController.class)
@Import(JacksonConfig.class)
class JsonDuplicadosTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JuegoServicio juego;

    @Test
    void claveJsonRepetidaDa409() throws Exception {
        String cuerpo = "{\"nombre\":\"Ana\",\"nombre\":\"Bob\",\"puntos_para_ganar\":5}";
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void cuerpoMalFormadoSigueDando400() throws Exception {
        String cuerpo = "{\"nombre\":";
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
