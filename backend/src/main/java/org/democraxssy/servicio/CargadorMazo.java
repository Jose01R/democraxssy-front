package org.democraxssy.servicio;

import java.util.ArrayList;
import java.util.List;

import org.democraxssy.dominio.CartaBlanca;
import org.democraxssy.dominio.CartaNegra;
import org.democraxssy.dominio.Mazos;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Carga y valida las barajas desde el JSON de {@code cartas.json}.
 *
 * <p>Reglas: cada carta negra tiene {@code texto} no vacio y {@code elegir} en 1..2, con
 * tantos huecos {@code ____} como {@code elegir}; cada blanca tiene {@code id} entero >= 0 y
 * {@code texto} no vacio. Ante cualquier incumplimiento lanza {@link MazoInvalidoException}
 * (fallar rapido al arrancar).
 */
public final class CargadorMazo {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HUECO = "____";

    private CargadorMazo() {
    }

    public static Mazos desdeJson(String json) {
        JsonNode raiz;
        try {
            raiz = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new MazoInvalidoException("No se pudo leer el JSON del mazo: " + e.getMessage());
        }
        List<CartaNegra> negras = leerNegras(raiz.get("negras"));
        List<CartaBlanca> blancas = leerBlancas(raiz.get("blancas"));
        if (negras.isEmpty() || blancas.isEmpty()) {
            throw new MazoInvalidoException("El mazo necesita al menos una carta negra y una blanca.");
        }
        return new Mazos(negras, blancas);
    }

    private static List<CartaNegra> leerNegras(JsonNode arreglo) {
        if (arreglo == null || !arreglo.isArray()) {
            throw new MazoInvalidoException("Falta el arreglo 'negras'.");
        }
        List<CartaNegra> negras = new ArrayList<>();
        for (JsonNode nodo : arreglo) {
            String texto = textoObligatorio(nodo, "negra");
            if (!nodo.path("elegir").isInt()) {
                throw new MazoInvalidoException("Carta negra sin 'elegir' entero: " + texto);
            }
            int elegir = nodo.get("elegir").asInt();
            if (elegir < 1 || elegir > 2) {
                throw new MazoInvalidoException("'elegir' debe ser 1 o 2: " + texto);
            }
            int huecos = contarHuecos(texto);
            if (huecos != elegir) {
                throw new MazoInvalidoException(
                        "La carta negra tiene " + huecos + " huecos pero elegir=" + elegir + ": " + texto);
            }
            negras.add(new CartaNegra(texto, elegir));
        }
        return negras;
    }

    private static List<CartaBlanca> leerBlancas(JsonNode arreglo) {
        if (arreglo == null || !arreglo.isArray()) {
            throw new MazoInvalidoException("Falta el arreglo 'blancas'.");
        }
        List<CartaBlanca> blancas = new ArrayList<>();
        for (JsonNode nodo : arreglo) {
            String texto = textoObligatorio(nodo, "blanca");
            JsonNode id = nodo.path("id");
            if (!id.isInt() || id.asInt() < 0) {
                throw new MazoInvalidoException("Carta blanca sin 'id' entero >= 0: " + texto);
            }
            blancas.add(new CartaBlanca(id.asInt(), texto));
        }
        return blancas;
    }

    private static String textoObligatorio(JsonNode nodo, String tipo) {
        JsonNode texto = nodo.path("texto");
        if (!texto.isTextual() || texto.asText().isBlank()) {
            throw new MazoInvalidoException("Carta " + tipo + " con 'texto' vacio o ausente.");
        }
        return texto.asText();
    }

    private static int contarHuecos(String texto) {
        return texto.split(HUECO, -1).length - 1;
    }
}
