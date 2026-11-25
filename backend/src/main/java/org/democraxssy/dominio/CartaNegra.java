package org.democraxssy.dominio;

/**
 * Carta negra: el texto con huecos marcados con {@code ____}. {@code elegir} es
 * cuantas cartas blancas pide (1 o 2) y coincide con la cantidad de huecos.
 */
public record CartaNegra(String texto, int elegir) {
}
