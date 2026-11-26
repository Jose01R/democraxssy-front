package org.democraxssy.repositorio;

import org.democraxssy.dominio.Partida;

/** Almacen de partidas. La implementacion vive en memoria (sin base de datos). */
public interface RepositorioPartidas {

    void guardar(Partida partida);

    /** Devuelve la partida o lanza {@code NoEncontradoException} si no existe. */
    Partida obtener(String id);

    void eliminar(String id);
}
