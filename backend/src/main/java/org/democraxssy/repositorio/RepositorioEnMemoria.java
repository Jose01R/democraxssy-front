package org.democraxssy.repositorio;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.democraxssy.dominio.Partida;
import org.democraxssy.errores.NoEncontradoException;

/** Partidas guardadas en un {@link ConcurrentHashMap}. La Fase E agrega la limpieza. */
public class RepositorioEnMemoria implements RepositorioPartidas {

    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();

    @Override
    public void guardar(Partida partida) {
        partidas.put(partida.id(), partida);
    }

    @Override
    public Partida obtener(String id) {
        Partida partida = partidas.get(id);
        if (partida == null) {
            throw new NoEncontradoException("Partida no encontrada.");
        }
        partida.ultimaActividad(Instant.now()); // toda operacion pasa por aqui: renueva actividad
        return partida;
    }

    @Override
    public void eliminar(String id) {
        partidas.remove(id);
    }

    @Override
    public int eliminarInactivas(Instant limite) {
        int antes = partidas.size();
        partidas.values().removeIf(partida -> partida.ultimaActividad().isBefore(limite));
        return antes - partidas.size();
    }
}
