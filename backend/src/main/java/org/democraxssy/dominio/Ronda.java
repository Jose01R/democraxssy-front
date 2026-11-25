package org.democraxssy.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Estado de una ronda (datos). En la Tarea 8+ se le agregan votos, favoritas y ganadora.
 */
public class Ronda {

    private final int numero;
    private final String soberanoId;
    private final CartaNegra cartaNegra;
    private final List<Respuesta> respuestas = new ArrayList<>();

    public Ronda(int numero, String soberanoId, CartaNegra cartaNegra) {
        this.numero = numero;
        this.soberanoId = soberanoId;
        this.cartaNegra = cartaNegra;
    }

    public int numero() {
        return numero;
    }

    public String soberanoId() {
        return soberanoId;
    }

    public CartaNegra cartaNegra() {
        return cartaNegra;
    }

    public List<Respuesta> respuestas() {
        return respuestas;
    }

    public void agregarRespuesta(Respuesta respuesta) {
        respuestas.add(respuesta);
    }

    public boolean yaRespondio(String jugadorId) {
        return respuestas.stream().anyMatch(r -> r.autorId().equals(jugadorId));
    }

    /** Se llama al abrir la votacion: fija un orden barajado y anonimo. */
    public void barajarRespuestas(Random rng) {
        Collections.shuffle(respuestas, rng);
    }
}
