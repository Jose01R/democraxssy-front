package org.democraxssy.repositorio;

import java.time.Instant;

import org.democraxssy.dominio.Partida;

/** Almacen de partidas. La implementacion vive en memoria (sin base de datos). */
public interface RepositorioPartidas {

    void guardar(Partida partida);

    /** Devuelve la partida o lanza {@code NoEncontradoException} si no existe. */
    Partida obtener(String id);

    void eliminar(String id);

    /**
     * Elimina las partidas cuya ultima actividad es anterior a {@code limite} (abandonadas).
     *
     * @return cuantas se eliminaron
     */
    int eliminarInactivas(Instant limite);
}
