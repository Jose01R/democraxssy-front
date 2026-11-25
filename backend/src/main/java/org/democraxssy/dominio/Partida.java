package org.democraxssy.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de una partida (datos). La logica (crear, unirse, iniciar, ...) vive en el servicio;
 * aqui solo se guardan los datos y mutaciones basicas.
 */
public class Partida {

    private final String id;
    private final String anfitrionId;
    private final int puntosParaGanar;
    private Fase fase = Fase.SALA;
    private final List<Jugador> jugadores = new ArrayList<>(); // orden de llegada
    private Mazo<CartaBlanca> mazoBlancas;
    private Mazo<CartaNegra> mazoNegras;
    private Ronda ronda;
    private final List<ResultadoRonda> historial = new ArrayList<>();
    private String ganadorId;

    public Partida(String id, String anfitrionId, int puntosParaGanar) {
        this.id = id;
        this.anfitrionId = anfitrionId;
        this.puntosParaGanar = puntosParaGanar;
    }

    public String id() {
        return id;
    }

    public String anfitrionId() {
        return anfitrionId;
    }

    public int puntosParaGanar() {
        return puntosParaGanar;
    }

    public Fase fase() {
        return fase;
    }

    public void fase(Fase fase) {
        this.fase = fase;
    }

    /** Jugadores en orden de llegada (el primero es el anfitrion). */
    public List<Jugador> jugadores() {
        return jugadores;
    }

    public void agregarJugador(Jugador jugador) {
        jugadores.add(jugador);
    }

    public boolean esAnfitrion(Jugador jugador) {
        return jugador.id().equals(anfitrionId);
    }

    public void iniciarMazos(Mazo<CartaBlanca> mazoBlancas, Mazo<CartaNegra> mazoNegras) {
        this.mazoBlancas = mazoBlancas;
        this.mazoNegras = mazoNegras;
    }

    public Mazo<CartaBlanca> mazoBlancas() {
        return mazoBlancas;
    }

    public Mazo<CartaNegra> mazoNegras() {
        return mazoNegras;
    }

    public Ronda ronda() {
        return ronda;
    }

    public void ronda(Ronda ronda) {
        this.ronda = ronda;
    }

    public List<ResultadoRonda> historial() {
        return historial;
    }

    public void agregarAlHistorial(ResultadoRonda resultado) {
        historial.add(resultado);
    }

    public String ganadorId() {
        return ganadorId;
    }

    public void ganadorId(String ganadorId) {
        this.ganadorId = ganadorId;
    }
}
