package org.democraxssy.controllers;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.democraxssy.dto.Vista;
import org.democraxssy.servicio.JuegoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RondaController.class)
class RondaControllerTest {

    private static final String TOKEN = "a".repeat(64);
    private static final String BEARER = "Bearer " + TOKEN;
    private static final Vista VISTA =
            new Vista("p1", "respuestas", null, 5, List.of(), null, null, List.of(), null);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JuegoServicio juego;

    @Test
    void iniciar() throws Exception {
        when(juego.iniciar(eq("p1"), eq(TOKEN))).thenReturn(VISTA);
        mvc.perform(post("/api/partidas/p1/iniciar").header("Authorization", BEARER))
                .andExpect(status().isOk());
        verify(juego).iniciar("p1", TOKEN);
    }

    @Test
    void responder() throws Exception {
        when(juego.responder(eq("p1"), eq(TOKEN), anyList())).thenReturn(VISTA);
        mvc.perform(put("/api/partidas/p1/respuestas")
                .header("Authorization", BEARER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartas\":[1,2]}"))
                .andExpect(status().isOk());
        verify(juego).responder("p1", TOKEN, List.of(1, 2));
    }

    @Test
    void votar() throws Exception {
        when(juego.votar(eq("p1"), eq(TOKEN), eq("r9"))).thenReturn(VISTA);
        mvc.perform(put("/api/partidas/p1/votos")
                .header("Authorization", BEARER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"respuesta_id\":\"r9\"}"))
                .andExpect(status().isOk());
        verify(juego).votar("p1", TOKEN, "r9");
    }

    @Test
    void decidir() throws Exception {
        when(juego.decidir(eq("p1"), eq(TOKEN), eq("r9"))).thenReturn(VISTA);
        mvc.perform(post("/api/partidas/p1/decision")
                .header("Authorization", BEARER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"respuesta_id\":\"r9\"}"))
                .andExpect(status().isOk());
        verify(juego).decidir("p1", TOKEN, "r9");
    }

    @Test
    void avanzar() throws Exception {
        when(juego.avanzar(eq("p1"), eq(TOKEN))).thenReturn(VISTA);
        mvc.perform(post("/api/partidas/p1/avanzar").header("Authorization", BEARER))
                .andExpect(status().isOk());
        verify(juego).avanzar("p1", TOKEN);
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(post("/api/partidas/p1/avanzar"))
                .andExpect(status().isUnauthorized());
    }
}
