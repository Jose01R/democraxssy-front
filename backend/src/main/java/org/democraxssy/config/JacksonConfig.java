package org.democraxssy.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Ajustes del lector JSON de entrada para que un cuerpo inesperado se rechace en vez de
 * aceptarse en silencio. Todo lo que aqui falla en el parseo o el bind cae en
 * {@link org.democraxssy.web.ManejadorErrores#handleHttpMessageNotReadable} → 409 (Conflicto),
 * que es como el contrato clasifica un cuerpo JSON invalido o inesperado.
 */
@Configuration
public class JacksonConfig {

    /** Claves repetidas ({@code {"nombre":"a","nombre":"b"}}) → error de parseo. */
    @Bean
    JsonMapperBuilderCustomizer deteccionDeClavesRepetidas() {
        return builder -> builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
    }

    /**
     * Rechaza cuerpos inesperados:
     * <ul>
     *   <li>campos que el DTO no declara (p. ej. {@code {"nombre":"Ana","admin":true}});</li>
     *   <li>un valor de tipo incorrecto donde se espera texto ({@code {"nombre":5}}): sin esto,
     *       Jackson lo convertiria a {@code "5"} en silencio.</li>
     * </ul>
     */
    @Bean
    JsonMapperBuilderCustomizer rechazoDeCuerposInesperados() {
        return builder -> {
            builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.withCoercionConfig(LogicalType.Textual, cfg -> {
                cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                cfg.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
                cfg.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
            });
            // Un entero no se acepta como texto ("5") ni como decimal (5.5). Cubre
            // puntos_para_ganar y los elementos de cartas.
            builder.withCoercionConfig(LogicalType.Integer, cfg -> {
                cfg.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
                cfg.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            });
        };
    }
}
