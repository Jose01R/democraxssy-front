package org.democraxssy.servicio;

import java.util.List;
import java.util.Optional;

import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.springframework.stereotype.Service;

/** Temporal: se borra cuando exista JuegoEnMemoria. */
@Service
public class JuegoServicioPendiente implements JuegoServicio {

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("JuegoEnMemoria aun no implementado");
    }

    public Sesion crearPartida(String nombre, Integer puntosParaGanar) { throw pendiente(); }

    public Sesion unirse(String partidaId, String nombre) { throw pendiente(); }

    public Vista obtenerPartida(String partidaId, Optional<String> token) { throw pendiente(); }

    public void eliminarPartida(String partidaId, String token) { throw pendiente(); }

    public Vista iniciar(String partidaId, String token) { throw pendiente(); }

    public Vista responder(String partidaId, String token, List<Integer> cartas) { throw pendiente(); }

    public Vista votar(String partidaId, String token, String respuestaId) { throw pendiente(); }

    public Vista decidir(String partidaId, String token, String respuestaId) { throw pendiente(); }

    public Vista avanzar(String partidaId, String token) { throw pendiente(); }
}
