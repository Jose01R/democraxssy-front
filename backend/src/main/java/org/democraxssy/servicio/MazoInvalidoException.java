package org.democraxssy.servicio;

/**
 * El {@code cartas.json} no cumple las reglas del mazo (campos faltantes, {@code elegir}
 * fuera de 1..2, o numero de huecos {@code ____} distinto de {@code elegir}).
 *
 * <p>Se usa para fallar rapido al arrancar, no es un error HTTP del juego.
 */
public class MazoInvalidoException extends RuntimeException {

    public MazoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
