package org.democraxssy.dominio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Mazo con monton de robo y de descartes.
 *
 * <p>Al robar con el robo vacio, rebaraja los descartes y los vuelve robo: asi el
 * mazo nunca se agota mientras haya cartas en juego. La partida termina solo por
 * puntos, nunca por falta de cartas. Solo lanza si no queda ninguna carta en ningun
 * monton (mazo de tamano cero: mazo invalido).
 *
 * @param <T> tipo de carta ({@link CartaBlanca} o {@link CartaNegra}).
 */
public final class Mazo<T> {

    private final Deque<T> robo = new ArrayDeque<>();
    private final List<T> descartes = new ArrayList<>();
    private final Random rng;

    public Mazo(List<T> cartas, Random rng) {
        this.rng = rng;
        List<T> copia = new ArrayList<>(cartas);
        Collections.shuffle(copia, rng);
        robo.addAll(copia);
    }

    /** Roba una carta; recicla los descartes si el robo esta vacio. */
    public T robar() {
        if (robo.isEmpty()) {
            reciclarDescartes();
        }
        if (robo.isEmpty()) {
            throw new IllegalStateException("Mazo sin cartas: ni robo ni descartes.");
        }
        return robo.pollFirst();
    }

    public void descartar(T carta) {
        descartes.add(carta);
    }

    public void descartar(Collection<T> cartas) {
        descartes.addAll(cartas);
    }

    /** Cartas que siguen en circulacion (robo + descartes). */
    public int disponibles() {
        return robo.size() + descartes.size();
    }

    private void reciclarDescartes() {
        if (descartes.isEmpty()) {
            return;
        }
        Collections.shuffle(descartes, rng);
        robo.addAll(descartes);
        descartes.clear();
    }
}
