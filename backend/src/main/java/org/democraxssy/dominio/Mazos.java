package org.democraxssy.dominio;

import java.util.List;

/** Las dos barajas cargadas del {@code cartas.json}: negras y blancas. */
public record Mazos(List<CartaNegra> negras, List<CartaBlanca> blancas) {
}
