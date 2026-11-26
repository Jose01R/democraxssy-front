package org.democraxssy.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.democraxssy.dominio.Mazos;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.democraxssy.repositorio.RepositorioPartidas;
import org.democraxssy.servicio.CargadorMazo;
import org.democraxssy.servicio.JuegoEnMemoria;
import org.democraxssy.servicio.JuegoServicio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wiring del juego: carga el mazo desde {@code resources/mazos/cartas.json} al arrancar
 * (fallando rapido si es invalido) y expone {@link JuegoEnMemoria} como el bean
 * {@link JuegoServicio} que usan los controllers.
 *
 * <p>{@code @EnableScheduling} activa el barrido de limpieza de la Fase E
 * ({@link org.democraxssy.servicio.LimpiezaPartidas}).
 */
@Configuration
@EnableScheduling
public class JuegoConfig {

    @Bean
    RepositorioPartidas repositorioPartidas() {
        return new RepositorioEnMemoria();
    }

    @Bean
    Mazos mazos() throws IOException {
        var recurso = new ClassPathResource("mazos/cartas.json");
        String json = new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return CargadorMazo.desdeJson(json);
    }

    @Bean
    JuegoServicio juegoServicio(RepositorioPartidas repositorio, Mazos mazos) {
        return new JuegoEnMemoria(repositorio, mazos, new SecureRandom());
    }
}
