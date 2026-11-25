package org.democraxssy.controllers;

import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.servicio.JuegoServicio;
import org.democraxssy.web.Autorizacion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/partidas")
public class PartidasController {

    public record NuevaPartida(String nombre, @JsonProperty("puntos_para_ganar") Integer puntosParaGanar) {
    }

    public record NuevoJugador(String nombre) {
    }

    private final JuegoServicio juego;

    public PartidasController(JuegoServicio juego) {
        this.juego = juego;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Sesion crear(@RequestBody NuevaPartida cuerpo, HttpServletRequest peticion) {
        Autorizacion.prohibida(peticion);
        return juego.crearPartida(cuerpo.nombre(), cuerpo.puntosParaGanar());
    }

    @PostMapping("/{id}/jugadores")
    @ResponseStatus(HttpStatus.CREATED)
    public Sesion unirse(@PathVariable String id, @RequestBody NuevoJugador cuerpo, HttpServletRequest peticion) {
        Autorizacion.prohibida(peticion);
        return juego.unirse(id, cuerpo.nombre());
    }

    @GetMapping("/{id}")
    public Vista obtener(@PathVariable String id, HttpServletRequest peticion) {
        return juego.obtenerPartida(id, Autorizacion.tokenOpcional(peticion));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id, HttpServletRequest peticion) {
        juego.eliminarPartida(id, Autorizacion.tokenObligatorio(peticion));
    }
}
