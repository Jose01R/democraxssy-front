package org.democraxssy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumenRonda(
        int numero,
        @JsonProperty("soberano_id") String soberanoId,
        @JsonProperty("ganador_id") String ganadorId,
        String frase,
        @JsonProperty("respeto_al_pueblo") boolean respetoAlPueblo) {
}
