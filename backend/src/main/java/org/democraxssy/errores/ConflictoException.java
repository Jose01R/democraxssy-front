package org.democraxssy.errores;

/** Accion fuera de fase o conflicto de estado (voto duplicado, partida llena, nombre repetido). HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
