package org.democraxssy.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.StreamReadFeature;

/**
 * Ajustes del lector JSON de entrada.
 *
 * <p>Activa la deteccion estricta de claves repetidas: un cuerpo como
 * {@code {"nombre":"a","nombre":"b"}} deja de aceptarse en silencio (Jackson, por
 * defecto, se queda con el ultimo valor) y lanza un error de parseo. El
 * {@link org.democraxssy.web.ManejadorErrores} lo traduce a 409 (Conflicto), que es
 * como el contrato clasifica un cuerpo JSON inesperado.
 */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer deteccionDeClavesRepetidas() {
        return builder -> builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
    }
}
