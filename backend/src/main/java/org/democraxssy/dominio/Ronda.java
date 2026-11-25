package org.democraxssy.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Estado de una ronda (datos). */
public class Ronda {

    private final int numero;
    private final String soberanoId;
    private final CartaNegra cartaNegra;
    private final List<Respuesta> respuestas = new ArrayList<>();
    private final Map<String, String> votos = new LinkedHashMap<>(); // jugadorId -> respuestaId
    private List<String> favoritas = List.of();
    private String ganadora;

    public Ronda(int numero, String soberanoId, CartaNegra cartaNegra) {
        this.numero = numero;
        this.soberanoId = soberanoId;
        this.cartaNegra = cartaNegra;
    }

    public int numero() {
        return numero;
    }

    public String soberanoId() {
        return soberanoId;
    }

    public CartaNegra cartaNegra() {
        return cartaNegra;
    }

    public List<Respuesta> respuestas() {
        return respuestas;
    }

    public void agregarRespuesta(Respuesta respuesta) {
        respuestas.add(respuesta);
    }

    public boolean yaRespondio(String jugadorId) {
        return respuestas.stream().anyMatch(r -> r.autorId().equals(jugadorId));
    }

    /** Se llama al abrir la votacion: fija un orden barajado y anonimo. */
    public void barajarRespuestas(Random rng) {
        Collections.shuffle(respuestas, rng);
    }

    public void votar(String jugadorId, String respuestaId) {
        votos.put(jugadorId, respuestaId);
    }

    public boolean yaVoto(String jugadorId) {
        return votos.containsKey(jugadorId);
    }

    public int totalVotos() {
        return votos.size();
    }

    public String votoDe(String jugadorId) {
        return votos.get(jugadorId);
    }

    public int votosDe(String respuestaId) {
        return (int) votos.values().stream().filter(respuestaId::equals).count();
    }

    public boolean existeRespuesta(String respuestaId) {
        return respuestas.stream().anyMatch(r -> r.id().equals(respuestaId));
    }

    public List<String> favoritas() {
        return favoritas;
    }

    public String ganadora() {
        return ganadora;
    }

    public void ganadora(String ganadora) {
        this.ganadora = ganadora;
    }

    /** Favoritas del pueblo = respuestas mas votadas (empates incluidas); vacio si nadie voto. */
    public void calcularFavoritas() {
        if (votos.isEmpty()) {
            favoritas = List.of();
            return;
        }
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (String respuestaId : votos.values()) {
            conteo.merge(respuestaId, 1L, Long::sum);
        }
        long max = conteo.values().stream().mapToLong(Long::longValue).max().orElse(0);
        favoritas = conteo.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();
    }
}
