package org.democraxssy.errores;

/** Token valido pero la accion no le corresponde a ese jugador. HTTP 403. */
public class ProhibidoException extends RuntimeException {
    public ProhibidoException(String mensaje) {
        super(mensaje);
    }
}
