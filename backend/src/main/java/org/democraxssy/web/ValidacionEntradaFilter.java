package org.democraxssy.web;

import java.io.IOException;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rechaza con 409 dos formas de solicitud inesperada que el contrato no admite:
 * <ul>
 *   <li>parámetros de consulta en cualquier ruta (el contrato no los admite en ninguna);</li>
 *   <li>cabeceras de override de método ({@code X-HTTP-Method-Override} y similares), que
 *       intentan disfrazar el método real;</li>
 *   <li>{@code GET}, {@code HEAD} o {@code DELETE} que traen cuerpo o {@code Content-Type}
 *       (esos métodos no llevan ninguno de los dos).</li>
 * </ul>
 *
 * <p>Corre después del {@link CorsFilter} (menor precedencia) para que la respuesta 409 también
 * lleve las cabeceras CORS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ValidacionEntradaFilter extends OncePerRequestFilter {

    private static final Set<String> CABECERAS_OVERRIDE =
            Set.of("X-HTTP-Method-Override", "X-HTTP-Method", "X-Method-Override");
    private static final Set<String> SIN_CUERPO = Set.of("GET", "HEAD", "DELETE");
    private static final String CUERPO_409 =
            "{\"error\":\"Solicitud inesperada.\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {

        if (peticion.getQueryString() != null || tieneOverride(peticion) || cuerpoNoPermitido(peticion)) {
            responder409(respuesta);
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }

    private static boolean tieneOverride(HttpServletRequest peticion) {
        for (String cabecera : CABECERAS_OVERRIDE) {
            if (peticion.getHeader(cabecera) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean cuerpoNoPermitido(HttpServletRequest peticion) {
        if (!SIN_CUERPO.contains(peticion.getMethod())) {
            return false;
        }
        return peticion.getContentType() != null || peticion.getContentLengthLong() > 0;
    }

    private static void responder409(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(409);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(CUERPO_409);
    }
}
