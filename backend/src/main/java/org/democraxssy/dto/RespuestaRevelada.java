package org.democraxssy.dto;

import java.util.List;

/** Respuesta mostrada desde la fase votacion (barajada y anonima). */
public record RespuestaRevelada(
        String id,
        List<String> cartas,
        String frase,
        boolean esMia,
        Integer votos,
        String autorId) {
}
