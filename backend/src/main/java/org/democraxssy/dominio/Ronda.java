package org.democraxssy.dominio;

/**
 * Estado de una ronda (datos). En la Tarea 7+ se le agregan respuestas, votos,
 * favoritas y ganadora.
 */
public class Ronda {

    private final int numero;
    private final String soberanoId;
    private final CartaNegra cartaNegra;

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
}
