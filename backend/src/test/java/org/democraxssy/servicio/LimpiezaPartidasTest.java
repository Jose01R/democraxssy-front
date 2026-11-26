package org.democraxssy.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.democraxssy.repositorio.RepositorioPartidas;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LimpiezaPartidasTest {

    @Test
    void limpiarBorraLasInactivasSegunElTimeout() {
        RepositorioPartidas repo = mock(RepositorioPartidas.class);
        LimpiezaPartidas limpieza = new LimpiezaPartidas(repo, Duration.ofMinutes(30));

        Instant antes = Instant.now().minus(30, ChronoUnit.MINUTES);
        limpieza.limpiar();
        Instant despues = Instant.now().minus(30, ChronoUnit.MINUTES);

        ArgumentCaptor<Instant> corte = ArgumentCaptor.forClass(Instant.class);
        verify(repo).eliminarInactivas(corte.capture());
        // el corte debe ser "ahora menos 30 min": entre los dos instantes medidos
        assertFalse(corte.getValue().isBefore(antes), "el corte no debe ser mas viejo que ahora-30m");
        assertFalse(corte.getValue().isAfter(despues), "el corte no debe ser mas nuevo que ahora-30m");
    }
}
