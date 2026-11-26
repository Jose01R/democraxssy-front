package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.democraxssy.dominio.CartaBlanca;
import org.democraxssy.dominio.CartaNegra;
import org.democraxssy.dominio.Mazos;
import org.democraxssy.dto.Sesion;
import org.democraxssy.repositorio.RepositorioEnMemoria;
import org.junit.jupiter.api.Test;

class ConcurrenciaTest {

    private JuegoServicio juego() {
        List<CartaNegra> negras = List.of(new CartaNegra("a ____", 1));
        List<CartaBlanca> blancas = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            blancas.add(new CartaBlanca(i, "c" + i));
        }
        return new JuegoEnMemoria(new RepositorioEnMemoria(), new Mazos(negras, blancas), new Random(1));
    }

    @Test
    void obtenerPartidaNoRompeMientrasOtrosSeUnen() throws Exception {
        JuegoServicio j = juego();
        Sesion anfitrion = j.crearPartida("Ana", 5);
        String pid = anfitrion.partidaId();

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        List<Future<?>> tareas = new ArrayList<>();

        // 21 jugadores se unen (llegan a 22, sin conflictos)
        for (int i = 1; i <= 21; i++) {
            final String nombre = "N" + i;
            tareas.add(pool.submit(() -> {
                await(salida);
                try {
                    j.unirse(pid, nombre);
                } catch (Throwable t) {
                    fallo.compareAndSet(null, t);
                }
            }));
        }
        // 4 hilos consultando en bucle mientras los demas mutan
        for (int q = 0; q < 4; q++) {
            tareas.add(pool.submit(() -> {
                await(salida);
                for (int k = 0; k < 300; k++) {
                    try {
                        j.obtenerPartida(pid, Optional.empty());
                    } catch (Throwable t) {
                        fallo.compareAndSet(null, t);
                    }
                }
            }));
        }

        salida.countDown(); // arrancan todos a la vez
        for (Future<?> t : tareas) {
            t.get();
        }
        pool.shutdown();

        assertNull(fallo.get(), () -> "obtenerPartida lanzo: " + fallo.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
