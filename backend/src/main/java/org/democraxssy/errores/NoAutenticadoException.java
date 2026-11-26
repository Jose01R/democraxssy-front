package org.democraxssy.errores;

/** Token ausente, mal formado, o valido pero ajeno a la partida. HTTP 401. */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
