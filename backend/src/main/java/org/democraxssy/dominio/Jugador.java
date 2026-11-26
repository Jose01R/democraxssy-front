package org.democraxssy.dominio;

import java.util.ArrayList;
import java.util.List;

/** Jugador de una partida (datos). El token es secreto y no sale en la vista publica. */
public class Jugador {

    private final String id;
    private final String nombre;
    private final String token;
    private int puntos;
    private final List<CartaBlanca> mano = new ArrayList<>();

    public Jugador(String id, String nombre, String token) {
        this.id = id;
        this.nombre = nombre;
        this.token = token;
    }

    public String id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String token() {
        return token;
    }

    public int puntos() {
        return puntos;
    }

    public void sumarPunto() {
        puntos++;
    }

    public List<CartaBlanca> mano() {
        return mano;
    }
}
