package org.democraxssy.servicio;

import java.util.List;
import java.util.Optional;

import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;

/**
 * Las operaciones del juego (acordadas con Joel para dividir la Fase C).
 *
 * <p>Devuelve los DTOs del contrato ({@link Sesion}, {@link Vista}) ya filtrados por jugador.
 * Es interfaz para que la capa web la pueda mockear; la implementacion es {@link JuegoEnMemoria}.
 */
public interface JuegoServicio {

    // --- Fase C: controllers de Joel ---
    Sesion crearPartida(String nombre, Integer puntosParaGanar);

    Sesion unirse(String partidaId, String nombre);

    Vista obtenerPartida(String partidaId, Optional<String> token);

    void eliminarPartida(String partidaId, String token);

    // --- Fase C: controllers mios ---
    Vista iniciar(String partidaId, String token);

    Vista responder(String partidaId, String token, List<Integer> cartas);

    Vista votar(String partidaId, String token, String respuestaId);

    Vista decidir(String partidaId, String token, String respuestaId);

    Vista avanzar(String partidaId, String token);
}
