/**
 * Almacenamiento de partidas en memoria.
 *
 * <p>{@code RepositorioPartidas} (interface) y {@code RepositorioEnMemoria}
 * (sobre {@code ConcurrentHashMap}). La Fase E le agregara la limpieza de partidas
 * inactivas para no reventar los 256 MB.
 */
package org.democraxssy.repositorio;
