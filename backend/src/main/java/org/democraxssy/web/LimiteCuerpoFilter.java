package org.democraxssy.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rechaza con 413 cualquier cuerpo de solicitud mayor a 4 KB (lo maneja el backend, no Nginx).
 *
 * <p>Corre despues del {@link CorsFilter} (menor precedencia) para que la respuesta 413 tambien
 * lleve las cabeceras CORS. Chequea primero el Content-Length declarado y, si falta o miente
 * (chunked), lee el cuerpo hasta el limite + 1; si cabe, lo cachea y lo deja leer al controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LimiteCuerpoFilter extends OncePerRequestFilter {

    static final int MAX_BYTES = 4096;
    private static final String CUERPO_413 =
            "{\"error\":\"El cuerpo de la solicitud es demasiado grande.\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {

        if (peticion.getContentLengthLong() > MAX_BYTES) {
            responder413(respuesta);
            return;
        }
        byte[] cuerpo = leerHasta(peticion.getInputStream(), MAX_BYTES + 1);
        if (cuerpo.length > MAX_BYTES) {
            responder413(respuesta);
            return;
        }
        cadena.doFilter(new CuerpoCacheado(peticion, cuerpo), respuesta);
    }

    private static byte[] leerHasta(InputStream entrada, int limite) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int total = 0;
        int leidos;
        while (total < limite
                && (leidos = entrada.read(buffer, 0, Math.min(buffer.length, limite - total))) != -1) {
            salida.write(buffer, 0, leidos);
            total += leidos;
        }
        return salida.toByteArray();
    }

    private static void responder413(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(413);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(CUERPO_413);
    }

    /** Envuelve la peticion para servir el cuerpo ya leido y cacheado. */
    private static final class CuerpoCacheado extends HttpServletRequestWrapper {

        private final byte[] cuerpo;

        CuerpoCacheado(HttpServletRequest peticion, byte[] cuerpo) {
            super(peticion);
            this.cuerpo = cuerpo;
        }

        @Override
        public int getContentLength() {
            return cuerpo.length;
        }

        @Override
        public long getContentLengthLong() {
            return cuerpo.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream fuente = new ByteArrayInputStream(cuerpo);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return fuente.read();
                }

                @Override
                public boolean isFinished() {
                    return fuente.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // lectura sincronica; no se usa modo asincrono
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
