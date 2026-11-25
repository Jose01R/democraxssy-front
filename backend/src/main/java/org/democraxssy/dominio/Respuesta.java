package org.democraxssy.dominio;

import java.util.List;

/**
 * Respuesta de un jugador a la carta negra: las cartas blancas jugadas en orden.
 * En la Tarea 8 se le agregan los votos.
 */
public class Respuesta {

    private final String id;
    private final String autorId;
    private final List<CartaBlanca> cartas;

    public Respuesta(String id, String autorId, List<CartaBlanca> cartas) {
        this.id = id;
        this.autorId = autorId;
        this.cartas = cartas;
    }

    public String id() {
        return id;
    }

    public String autorId() {
        return autorId;
    }

    public List<CartaBlanca> cartas() {
        return cartas;
    }
}
