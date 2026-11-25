package org.democraxssy.servicio;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generador de identificadores secretos del juego, en hexadecimal minuscula.
 *
 * <p>Cada metodo devuelve el largo que pide el contrato:
 * partida 32, jugador 16, token 64, respuesta 12 caracteres hex.
 */
public final class Ids {

    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of(); // minuscula por defecto

    private Ids() {
    }

    /** 32 caracteres hex. */
    public static String partida() {
        return hex(16);
    }

    /** 16 caracteres hex. */
    public static String jugador() {
        return hex(8);
    }

    /** 64 caracteres hex. */
    public static String token() {
        return hex(32);
    }

    /** 12 caracteres hex. */
    public static String respuesta() {
        return hex(6);
    }

    private static String hex(int bytes) {
        byte[] buffer = new byte[bytes];
        RNG.nextBytes(buffer);
        return HEX.formatHex(buffer);
    }
}
