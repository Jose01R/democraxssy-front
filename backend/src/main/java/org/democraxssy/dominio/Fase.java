package org.democraxssy.dominio;

/** Fases de la maquina de estados de una partida. */
public enum Fase {
    SALA, RESPUESTAS, VOTACION, DECISION, RESULTADO, TERMINADA;

    /** Nombre como lo espera el contrato (minuscula): "sala", "respuestas", ... */
    public String contrato() {
        return name().toLowerCase();
    }
}
