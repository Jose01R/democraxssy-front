package org.democraxssy.errores;

/** La partida, la respuesta o la ruta no existen. HTTP 404. */
public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
