package org.democraxssy.controllers;

import java.util.List;

import org.democraxssy.dto.Vista;
import org.democraxssy.servicio.JuegoServicio;
import org.democraxssy.web.Autorizacion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Operaciones de ronda (acciones del jugador autenticado). Todas requieren token y
 * devuelven la vista actualizada. La logica vive en {@link JuegoServicio}.
 */
@RestController
@RequestMapping("/api/partidas")
public class RondaController {

    public record Jugada(List<Integer> cartas) {
    }

    public record Eleccion(@JsonProperty("respuesta_id") String respuestaId) {
    }

    private final JuegoServicio juego;

    public RondaController(JuegoServicio juego) {
        this.juego = juego;
    }

    @PostMapping("/{id}/iniciar")
    public Vista iniciar(@PathVariable String id, HttpServletRequest peticion) {
        return juego.iniciar(id, Autorizacion.tokenObligatorio(peticion));
    }

    @PutMapping("/{id}/respuestas")
    public Vista responder(@PathVariable String id, @RequestBody Jugada cuerpo, HttpServletRequest peticion) {
        return juego.responder(id, Autorizacion.tokenObligatorio(peticion), cuerpo.cartas());
    }

    @PutMapping("/{id}/votos")
    public Vista votar(@PathVariable String id, @RequestBody Eleccion cuerpo, HttpServletRequest peticion) {
        return juego.votar(id, Autorizacion.tokenObligatorio(peticion), cuerpo.respuestaId());
    }

    @PostMapping("/{id}/decision")
    public Vista decidir(@PathVariable String id, @RequestBody Eleccion cuerpo, HttpServletRequest peticion) {
        return juego.decidir(id, Autorizacion.tokenObligatorio(peticion), cuerpo.respuestaId());
    }

    @PostMapping("/{id}/avanzar")
    public Vista avanzar(@PathVariable String id, HttpServletRequest peticion) {
        return juego.avanzar(id, Autorizacion.tokenObligatorio(peticion));
    }
}
