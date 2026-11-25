package org.democraxssy.web;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CORS en TODAS las respuestas, incluidas las de error.
 *
 * <p>Se implementa como filtro de servlet con la maxima precedencia (y corriendo
 * tambien en el despacho de error) a proposito: asi las cabeceras CORS salen
 * aunque la peticion termine en 400/401/403/404/409/413/500, no solo en las 2xx.
 * Un {@code WebMvcConfigurer} con mapeo CORS no garantiza eso.
 *
 * <p>Origen {@code *}: la API es publica y la autenticacion es por token en la
 * cabecera {@code Authorization} (no por cookies), asi que no se necesita
 * {@code Allow-Credentials}. Si mas adelante hay que restringir origenes, se
 * cambia solo aqui.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, HEAD, OPTIONS");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");

        // Preflight: responder de una sin pasar por el resto de la cadena.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        chain.doFilter(request, response);
    }

    /** Correr tambien en el despacho de error, para que el 4xx/5xx lleve CORS. */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
