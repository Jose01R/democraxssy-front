package org.democraxssy.web;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.NoAutenticadoException;

import jakarta.servlet.http.HttpServletRequest;

public final class Autorizacion {

    private static final Pattern BEARER = Pattern.compile("^Bearer ([0-9a-f]{64})$");

    private Autorizacion() {
    }

    public static void prohibida(HttpServletRequest peticion) {
        if (peticion.getHeader("Authorization") != null) {
            throw new ConflictoException("Esta ruta no acepta Authorization");
        }
    }

    public static Optional<String> tokenOpcional(HttpServletRequest peticion) {
        List<String> valores = Collections.list(peticion.getHeaders("Authorization"));
        if (valores.isEmpty()) {
            return Optional.empty();
        }
        if (valores.size() > 1) {
            throw new ConflictoException("Authorization repetido");
        }
        Matcher m = BEARER.matcher(valores.get(0));
        if (!m.matches()) {
            throw new ConflictoException("Authorization mal formado");
        }
        return Optional.of(m.group(1));
    }

    public static String tokenObligatorio(HttpServletRequest peticion) {
        return tokenOpcional(peticion)
                .orElseThrow(() -> new NoAutenticadoException("Falta el token"));
    }
}
