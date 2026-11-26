package org.democraxssy.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.democraxssy.dto.CartaBlancaDto;
import org.democraxssy.dto.CartaNegraDto;
import org.democraxssy.dto.JugadorPublico;
import org.democraxssy.dto.RespuestaRevelada;
import org.democraxssy.dto.Ronda;
import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.dto.Yo;
import org.democraxssy.errores.DatosInvalidosException;
import org.democraxssy.errores.NoAutenticadoException;
import org.democraxssy.errores.NoEncontradoException;
import org.democraxssy.errores.ProhibidoException;
import org.democraxssy.servicio.JuegoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PartidasController.class)
class PartidasControllerTest {

    private static final String PARTIDA = "0123456789abcdef0123456789abcdef";
    private static final String JUGADOR = "0123456789abcdef";
    private static final String TOKEN = "ab".repeat(32);
    private static final String BEARER = "Bearer " + TOKEN;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JuegoServicio juego;

    private static Vista vistaLobby() {
        JugadorPublico anfitrion = new JugadorPublico(JUGADOR, "Ana", 0, true, false, false, false);
        Yo yo = new Yo(JUGADOR, "Ana", List.of(), null, false);
        return new Vista(PARTIDA, "lobby", JUGADOR, 5, List.of(anfitrion), null, yo, List.of(), null);
    }

    private static Vista vistaRespondiendo() {
        RespuestaRevelada oculta = new RespuestaRevelada("a1b2c3d4e5f6", List.of("Una"), "Frase", true, null, null);
        Ronda ronda = new Ronda(1, JUGADOR, new CartaNegraDto("___", 1), 1, 0, List.of(oculta), List.of(), null);
        Yo yo = new Yo(JUGADOR, "Ana", List.of(new CartaBlancaDto(7, "Una")), null, false);
        return new Vista(PARTIDA, "votacion", JUGADOR, 5, List.of(), ronda, yo, List.of(), null);
    }

    @Test
    void crearDevuelve201EnSnakeCase() throws Exception {
        when(juego.crearPartida("Ana", 5)).thenReturn(new Sesion(PARTIDA, JUGADOR, TOKEN, vistaLobby()));

        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"puntos_para_ganar\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partida_id").value(PARTIDA))
                .andExpect(jsonPath("$.jugador_id").value(JUGADOR))
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.partida.anfitrion_id").value(JUGADOR))
                .andExpect(jsonPath("$.partida.puntos_para_ganar").value(5))
                .andExpect(jsonPath("$.partida.jugadores[0].es_anfitrion").value(true))
                .andExpect(jsonPath("$.partida.ronda").isEmpty())
                .andExpect(jsonPath("$.partida.ganador_id").isEmpty())
                .andExpect(jsonPath("$.partida.yo.mi_voto").isEmpty());
    }

    @Test
    void crearSinPuntosPasaNull() throws Exception {
        when(juego.crearPartida("Ana", null)).thenReturn(new Sesion(PARTIDA, JUGADOR, TOKEN, vistaLobby()));

        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"))
                .andExpect(status().isCreated());
        verify(juego).crearPartida("Ana", null);
    }

    @Test
    void crearConAuthorizationDa409() throws Exception {
        mvc.perform(post("/api/partidas").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(juego);
    }

    @Test
    void crearConNombreInvalidoDa400() throws Exception {
        when(juego.crearPartida(anyString(), any())).thenThrow(new DatosInvalidosException("nombre"));

        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("nombre"));
    }

    @Test
    void crearConJsonRotoDa409() throws Exception {
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unirseDevuelve201() throws Exception {
        when(juego.unirse(PARTIDA, "Beto")).thenReturn(new Sesion(PARTIDA, JUGADOR, TOKEN, vistaLobby()));

        mvc.perform(post("/api/partidas/" + PARTIDA + "/jugadores")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Beto\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(TOKEN));
    }

    @Test
    void unirseConAuthorizationDa409() throws Exception {
        mvc.perform(post("/api/partidas/" + PARTIDA + "/jugadores").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Beto\"}"))
                .andExpect(status().isConflict());
        verifyNoInteractions(juego);
    }

    @Test
    void unirseAPartidaInexistenteDa404() throws Exception {
        when(juego.unirse(eq(PARTIDA), anyString())).thenThrow(new NoEncontradoException("partida"));

        mvc.perform(post("/api/partidas/" + PARTIDA + "/jugadores")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Beto\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerSinTokenPasaVacio() throws Exception {
        when(juego.obtenerPartida(PARTIDA, Optional.empty())).thenReturn(vistaLobby());

        mvc.perform(get("/api/partidas/" + PARTIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PARTIDA));
    }

    @Test
    void obtenerConTokenLoPasa() throws Exception {
        when(juego.obtenerPartida(PARTIDA, Optional.of(TOKEN))).thenReturn(vistaRespondiendo());

        mvc.perform(get("/api/partidas/" + PARTIDA).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ronda.carta_negra.elegir").value(1))
                .andExpect(jsonPath("$.ronda.respuestas[0].es_mia").value(true))
                .andExpect(jsonPath("$.ronda.respuestas[0].votos").doesNotExist())
                .andExpect(jsonPath("$.ronda.respuestas[0].autor_id").doesNotExist())
                .andExpect(jsonPath("$.ronda.ganadora").isEmpty())
                .andExpect(jsonPath("$.yo.mano[0].id").value(7));
    }

    @Test
    void obtenerConTokenMalFormadoDa409() throws Exception {
        mvc.perform(get("/api/partidas/" + PARTIDA).header("Authorization", "Bearer " + TOKEN.toUpperCase()))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/partidas/" + PARTIDA).header("Authorization", TOKEN))
                .andExpect(status().isConflict());
        verifyNoInteractions(juego);
    }

    @Test
    void obtenerConAuthorizationRepetidoDa409() throws Exception {
        mvc.perform(get("/api/partidas/" + PARTIDA).header("Authorization", BEARER, BEARER))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerConTokenAjenoDa401() throws Exception {
        when(juego.obtenerPartida(PARTIDA, Optional.of(TOKEN))).thenThrow(new NoAutenticadoException("token"));

        mvc.perform(get("/api/partidas/" + PARTIDA).header("Authorization", BEARER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete("/api/partidas/" + PARTIDA).header("Authorization", BEARER))
                .andExpect(status().isNoContent());
        verify(juego).eliminarPartida(PARTIDA, TOKEN);
    }

    @Test
    void eliminarSinTokenDa401() throws Exception {
        mvc.perform(delete("/api/partidas/" + PARTIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        verifyNoInteractions(juego);
    }

    @Test
    void eliminarSinSerAnfitrionDa403() throws Exception {
        doThrow(new ProhibidoException("anfitrion")).when(juego).eliminarPartida(PARTIDA, TOKEN);

        mvc.perform(delete("/api/partidas/" + PARTIDA).header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void errorInesperadoDa500SinDetalles() throws Exception {
        when(juego.obtenerPartida(PARTIDA, Optional.empty())).thenThrow(new IllegalStateException("detalle interno"));

        mvc.perform(get("/api/partidas/" + PARTIDA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void servicioPendienteDa500ConFormatoDelContrato() throws Exception {
        when(juego.crearPartida(anyString(), any())).thenThrow(new UnsupportedOperationException("pendiente"));

        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }

    @Test
    void contentTypeNoSoportadoDa409() throws Exception {
        mvc.perform(post("/api/partidas").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        verifyNoInteractions(juego);
    }

    @Test
    void metodoNoSoportadoNoEsUn500() throws Exception {
        mvc.perform(put("/api/partidas"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(juego);
    }

    @Test
    void crearSinNombreDa409() throws Exception {
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":null}"))
                .andExpect(status().isConflict());
        verifyNoInteractions(juego);
    }

    @Test
    void unirseSinNombreDa409() throws Exception {
        mvc.perform(post("/api/partidas/" + PARTIDA + "/jugadores")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        verifyNoInteractions(juego);
    }
}
