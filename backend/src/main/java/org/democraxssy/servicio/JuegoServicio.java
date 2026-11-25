package org.democraxssy.servicio;

import java.util.List;
import java.util.Optional;

import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;

public interface JuegoServicio {

    Sesion crearPartida(String nombre, Integer puntosParaGanar);

    Sesion unirse(String partidaId, String nombre);

    Vista obtenerPartida(String partidaId, Optional<String> token);

    void eliminarPartida(String partidaId, String token);

    Vista iniciar(String partidaId, String token);

    Vista responder(String partidaId, String token, List<Integer> cartas);

    Vista votar(String partidaId, String token, String respuestaId);

    Vista decidir(String partidaId, String token, String respuestaId);

    Vista avanzar(String partidaId, String token);
}
