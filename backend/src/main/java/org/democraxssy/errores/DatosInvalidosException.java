package org.democraxssy.errores;

/** Datos bien formados pero con valores invalidos para el juego. HTTP 400. */
public class DatosInvalidosException extends RuntimeException {
    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
