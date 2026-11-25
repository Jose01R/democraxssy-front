package org.democraxssy.dto;

import java.util.List;

import org.democraxssy.dominio.CartaBlanca;

/** Datos privados del jugador autenticado. */
public record Yo(
        String id,
        String nombre,
        List<CartaBlanca> mano,
        String miVoto,
        boolean puedeAvanzar) {
}
