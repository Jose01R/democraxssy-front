package org.democraxssy.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;

import org.democraxssy.dominio.CartaBlanca;
import org.democraxssy.dominio.CartaNegra;
import org.democraxssy.dominio.Fase;
import org.democraxssy.dominio.Jugador;
import org.democraxssy.dominio.Mazo;
import org.democraxssy.dominio.Mazos;
import org.democraxssy.dominio.Partida;
import org.democraxssy.dominio.Respuesta;
import org.democraxssy.dominio.ResultadoRonda;
import org.democraxssy.dominio.Ronda;
import org.democraxssy.dto.CartaBlancaDto;
import org.democraxssy.dto.CartaNegraDto;
import org.democraxssy.dto.JugadorPublico;
import org.democraxssy.dto.RespuestaRevelada;
import org.democraxssy.dto.ResumenRonda;
import org.democraxssy.dto.Sesion;
import org.democraxssy.dto.Vista;
import org.democraxssy.dto.Yo;
import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.DatosInvalidosException;
import org.democraxssy.errores.NoAutenticadoException;
import org.democraxssy.errores.NoEncontradoException;
import org.democraxssy.errores.ProhibidoException;
import org.democraxssy.repositorio.RepositorioPartidas;

/**
 * Implementacion en memoria del juego. Las mutaciones de una partida se serializan con un lock
 * por partida (synchronized sobre el objeto Partida).
 */
public class JuegoEnMemoria implements JuegoServicio {

    private static final int MAX_JUGADORES = 22;
    private static final int PUNTOS_DEFECTO = 5;

    private final RepositorioPartidas repositorio;
    private final Mazos mazos;
    private final Random rng;

    public JuegoEnMemoria(RepositorioPartidas repositorio, Mazos mazos, Random rng) {
        this.repositorio = repositorio;
        this.mazos = mazos;
        this.rng = rng;
    }

    @Override
    public Sesion crearPartida(String nombre, Integer puntosParaGanar) {
        String nombreValido = validarNombre(nombre);
        int puntos = (puntosParaGanar == null) ? PUNTOS_DEFECTO : puntosParaGanar;
        if (puntos < 1 || puntos > 20) {
            throw new DatosInvalidosException("puntos_para_ganar debe estar entre 1 y 20.");
        }
        Jugador anfitrion = nuevoJugador(nombreValido);
        Partida partida = new Partida(org.democraxssy.servicio.Ids.partida(), anfitrion.id(), puntos);
        partida.agregarJugador(anfitrion);
        repositorio.guardar(partida);
        return sesionDe(partida, anfitrion);
    }

    @Override
    public Sesion unirse(String partidaId, String nombre) {
        Partida partida = repositorio.obtener(partidaId);
        synchronized (partida) {
            if (partida.fase() != Fase.SALA) {
                throw new ConflictoException("La partida ya inicio; no se puede unir.");
            }
            String nombreValido = validarNombre(nombre);
            if (partida.jugadores().size() >= MAX_JUGADORES) {
                throw new ConflictoException("La partida esta llena (" + MAX_JUGADORES + ").");
            }
            boolean repetido = partida.jugadores().stream()
                    .anyMatch(j -> j.nombre().equalsIgnoreCase(nombreValido));
            if (repetido) {
                throw new ConflictoException("Ya hay un jugador con ese nombre.");
            }
            Jugador jugador = nuevoJugador(nombreValido);
            partida.agregarJugador(jugador);
            return sesionDe(partida, jugador);
        }
    }

    @Override
    public Vista obtenerPartida(String partidaId, Optional<String> token) {
        Partida partida = repositorio.obtener(partidaId);
        Jugador yo = token.map(t -> jugadorPorToken(partida, t)).orElse(null);
        return vistaPara(partida, yo);
    }

    @Override
    public void eliminarPartida(String partidaId, String token) {
        Partida partida = repositorio.obtener(partidaId);
        Jugador yo = jugadorPorToken(partida, token);
        if (!partida.esAnfitrion(yo)) {
            throw new ProhibidoException("Solo el anfitrion puede eliminar la partida.");
        }
        if (partida.fase() != Fase.TERMINADA) {
            throw new ConflictoException("Solo se puede eliminar una partida terminada.");
        }
        repositorio.eliminar(partidaId);
    }

    private static final int CARTAS_EN_MANO = 7;

    @Override
    public Vista iniciar(String partidaId, String token) {
        Partida partida = repositorio.obtener(partidaId);
        synchronized (partida) {
            Jugador yo = jugadorPorToken(partida, token);
            if (!partida.esAnfitrion(yo)) {
                throw new ProhibidoException("Solo el anfitrion puede iniciar la partida.");
            }
            if (partida.fase() != Fase.SALA) {
                throw new ConflictoException("La partida ya inicio.");
            }
            int n = partida.jugadores().size();
            if (n < 4 || n > MAX_JUGADORES) {
                throw new ConflictoException("Se necesitan entre 4 y " + MAX_JUGADORES + " jugadores.");
            }
            if (n % 2 != 0) {
                throw new ConflictoException("El numero de jugadores debe ser par.");
            }
            Mazo<CartaBlanca> blancas = new Mazo<>(mazos.blancas(), rng);
            Mazo<CartaNegra> negras = new Mazo<>(mazos.negras(), rng);
            partida.iniciarMazos(blancas, negras);
            for (Jugador jugador : partida.jugadores()) {
                for (int i = 0; i < CARTAS_EN_MANO; i++) {
                    jugador.mano().add(blancas.robar());
                }
            }
            partida.ronda(new Ronda(1, partida.anfitrionId(), negras.robar()));
            partida.fase(Fase.RESPUESTAS);
            return vistaPara(partida, yo);
        }
    }

    @Override
    public Vista responder(String partidaId, String token, List<Integer> cartas) {
        Partida partida = repositorio.obtener(partidaId);
        synchronized (partida) {
            Jugador yo = jugadorPorToken(partida, token);
            if (partida.fase() != Fase.RESPUESTAS) {
                throw new ConflictoException("No es la fase de respuestas.");
            }
            Ronda ronda = partida.ronda();
            if (yo.id().equals(ronda.soberanoId())) {
                throw new ProhibidoException("El soberano no responde.");
            }
            if (ronda.yaRespondio(yo.id())) {
                throw new ConflictoException("Ya respondiste en esta ronda.");
            }
            int elegir = ronda.cartaNegra().elegir();
            validarCantidadDeCartas(cartas, elegir);
            List<CartaBlanca> jugadas = quitarDeLaMano(yo, cartas);
            ronda.agregarRespuesta(new Respuesta(Ids.respuesta(), yo.id(), jugadas));
            if (ronda.respuestas().size() == partida.jugadores().size() - 1) {
                ronda.barajarRespuestas(rng);
                partida.fase(Fase.VOTACION);
            }
            return vistaPara(partida, yo);
        }
    }

    private static void validarCantidadDeCartas(List<Integer> cartas, int elegir) {
        if (cartas == null || cartas.size() != elegir) {
            throw new DatosInvalidosException("Debes jugar exactamente " + elegir + " carta(s).");
        }
        if (cartas.stream().distinct().count() != cartas.size()) {
            throw new DatosInvalidosException("No puedes repetir cartas.");
        }
    }

    /** Resuelve todas las cartas en la mano (en orden) antes de quitarlas, para no dejar estado a medias. */
    private static List<CartaBlanca> quitarDeLaMano(Jugador yo, List<Integer> ids) {
        List<CartaBlanca> jugadas = new ArrayList<>();
        for (int id : ids) {
            CartaBlanca carta = yo.mano().stream()
                    .filter(c -> c.id() == id)
                    .findFirst()
                    .orElseThrow(() -> new DatosInvalidosException("La carta " + id + " no esta en tu mano."));
            jugadas.add(carta);
        }
        yo.mano().removeAll(jugadas);
        return jugadas;
    }

    @Override
    public Vista votar(String partidaId, String token, String respuestaId) {
        Partida partida = repositorio.obtener(partidaId);
        synchronized (partida) {
            Jugador yo = jugadorPorToken(partida, token);
            if (partida.fase() != Fase.VOTACION) {
                throw new ConflictoException("La votacion no esta abierta.");
            }
            Ronda ronda = partida.ronda();
            if (yo.id().equals(ronda.soberanoId())) {
                throw new ProhibidoException("El soberano no vota: ya tendra la ultima palabra.");
            }
            if (ronda.yaVoto(yo.id())) {
                throw new ConflictoException("Ya votaste en esta ronda.");
            }
            if (!ronda.existeRespuesta(respuestaId)) {
                throw new NoEncontradoException("Respuesta no encontrada.");
            }
            ronda.votar(yo.id(), respuestaId);
            if (ronda.totalVotos() == partida.jugadores().size() - 1) {
                ronda.calcularFavoritas();
                partida.fase(Fase.DECISION);
            }
            return vistaPara(partida, yo);
        }
    }

    @Override
    public Vista decidir(String partidaId, String token, String respuestaId) {
        Partida partida = repositorio.obtener(partidaId);
        synchronized (partida) {
            Jugador yo = jugadorPorToken(partida, token);
            if (partida.fase() != Fase.DECISION) {
                throw new ConflictoException("No es la fase de decision.");
            }
            Ronda ronda = partida.ronda();
            if (!yo.id().equals(ronda.soberanoId())) {
                throw new ProhibidoException("Solo el soberano decide la ganadora.");
            }
            Respuesta ganadora = ronda.respuestas().stream()
                    .filter(r -> r.id().equals(respuestaId))
                    .findFirst()
                    .orElseThrow(() -> new NoEncontradoException("Respuesta no encontrada."));
            ronda.ganadora(respuestaId);
            Jugador autor = jugadorPorId(partida, ganadora.autorId());
            autor.sumarPunto();
            boolean respeto = ronda.favoritas().isEmpty() || ronda.favoritas().contains(respuestaId);
            partida.agregarAlHistorial(new ResultadoRonda(
                    ronda.numero(), ronda.soberanoId(), autor.id(),
                    frase(ronda.cartaNegra(), ganadora.cartas()), respeto));
            if (autor.puntos() >= partida.puntosParaGanar()) {
                partida.ganadorId(autor.id());
                partida.fase(Fase.TERMINADA);
            } else {
                partida.fase(Fase.RESULTADO);
            }
            return vistaPara(partida, yo);
        }
    }

    @Override
    public Vista avanzar(String partidaId, String token) {
        Partida partida = repositorio.obtener(partidaId);
        synchronized (partida) {
            Jugador yo = jugadorPorToken(partida, token);
            Ronda ronda = partida.ronda();
            boolean esSoberano = ronda != null && yo.id().equals(ronda.soberanoId());
            if (!partida.esAnfitrion(yo) && !esSoberano) {
                throw new ProhibidoException("Solo el anfitrion o el soberano pueden avanzar.");
            }
            switch (partida.fase()) {
                case RESPUESTAS -> {
                    if (ronda.respuestas().size() < 2) {
                        throw new ConflictoException("Se necesitan al menos 2 respuestas para abrir la votacion.");
                    }
                    ronda.barajarRespuestas(rng);
                    partida.fase(Fase.VOTACION);
                }
                case VOTACION -> {
                    ronda.calcularFavoritas();
                    partida.fase(Fase.DECISION);
                }
                case RESULTADO -> iniciarSiguienteRonda(partida);
                default -> throw new ConflictoException("No se puede avanzar en la fase actual.");
            }
            return vistaPara(partida, yo);
        }
    }

    // ---- helpers ----

    private Jugador nuevoJugador(String nombre) {
        return new Jugador(Ids.jugador(), nombre, Ids.token());
    }

    private static String validarNombre(String nombre) {
        if (nombre == null) {
            throw new DatosInvalidosException("El nombre es obligatorio.");
        }
        String limpio = nombre.strip();
        if (limpio.isEmpty() || limpio.length() > 24) {
            throw new DatosInvalidosException("El nombre debe tener entre 1 y 24 caracteres.");
        }
        if (limpio.chars().anyMatch(Character::isISOControl)) {
            throw new DatosInvalidosException("El nombre no admite caracteres de control.");
        }
        return limpio;
    }

    /** Rota el soberano (orden de llegada, con vuelta), recicla cartas y repone manos a 7. */
    private void iniciarSiguienteRonda(Partida partida) {
        Ronda anterior = partida.ronda();
        for (Respuesta respuesta : anterior.respuestas()) {
            partida.mazoBlancas().descartar(respuesta.cartas());
        }
        partida.mazoNegras().descartar(anterior.cartaNegra());
        String siguienteSoberano = siguienteSoberano(partida, anterior.soberanoId());
        for (Jugador jugador : partida.jugadores()) {
            while (jugador.mano().size() < CARTAS_EN_MANO) {
                jugador.mano().add(partida.mazoBlancas().robar());
            }
        }
        partida.ronda(new Ronda(anterior.numero() + 1, siguienteSoberano, partida.mazoNegras().robar()));
        partida.fase(Fase.RESPUESTAS);
    }

    private static String siguienteSoberano(Partida partida, String actualId) {
        List<Jugador> jugadores = partida.jugadores();
        int indice = 0;
        for (int i = 0; i < jugadores.size(); i++) {
            if (jugadores.get(i).id().equals(actualId)) {
                indice = i;
                break;
            }
        }
        return jugadores.get((indice + 1) % jugadores.size()).id();
    }

    private Jugador jugadorPorId(Partida partida, String id) {
        return partida.jugadores().stream()
                .filter(j -> j.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoEncontradoException("Jugador no encontrado."));
    }

    private Jugador jugadorPorToken(Partida partida, String token) {
        return partida.jugadores().stream()
                .filter(j -> j.token().equals(token))
                .findFirst()
                .orElseThrow(() -> new NoAutenticadoException("Token invalido para esta partida."));
    }

    private Sesion sesionDe(Partida partida, Jugador yo) {
        return new Sesion(partida.id(), yo.id(), yo.token(), vistaPara(partida, yo));
    }

    private Vista vistaPara(Partida partida, Jugador yo) {
        Ronda ronda = partida.ronda();
        String soberanoId = (ronda == null) ? null : ronda.soberanoId();
        List<JugadorPublico> jugadores = partida.jugadores().stream()
                .map(j -> new JugadorPublico(
                        j.id(), j.nombre(), j.puntos(),
                        partida.esAnfitrion(j),
                        j.id().equals(soberanoId),
                        ronda != null && ronda.yaRespondio(j.id()),
                        ronda != null && ronda.yaVoto(j.id())))
                .toList();
        Yo yoDto = (yo == null) ? null
                : new Yo(yo.id(), yo.nombre(),
                        yo.mano().stream().map(c -> new CartaBlancaDto(c.id(), c.texto())).toList(),
                        (ronda == null) ? null : ronda.votoDe(yo.id()),
                        puedeAvanzar(partida, yo));
        return new Vista(
                partida.id(),
                partida.fase().contrato(),
                partida.anfitrionId(),
                partida.puntosParaGanar(),
                jugadores,
                rondaDto(partida, yo),
                yoDto,
                partida.historial().stream()
                        .map(h -> new ResumenRonda(
                                h.numero(), h.soberanoId(), h.ganadorId(), h.frase(), h.respetoAlPueblo()))
                        .toList(),
                partida.ganadorId());
    }

    private boolean puedeAvanzar(Partida partida, Jugador yo) {
        Ronda ronda = partida.ronda();
        return partida.esAnfitrion(yo) || (ronda != null && yo.id().equals(ronda.soberanoId()));
    }

    private org.democraxssy.dto.Ronda rondaDto(Partida partida, Jugador yo) {
        Ronda ronda = partida.ronda();
        if (ronda == null) {
            return null;
        }
        CartaNegra negra = ronda.cartaNegra();
        return new org.democraxssy.dto.Ronda(
                ronda.numero(),
                ronda.soberanoId(),
                new CartaNegraDto(negra.texto(), negra.elegir()),
                ronda.respuestas().size(),
                ronda.totalVotos(),
                revelarRespuestas(partida, ronda, yo),
                ronda.favoritas(),
                ronda.ganadora());
    }

    /** Revelado progresivo: ocultas en respuestas; anonimas desde votacion; votos desde decision; autor desde resultado. */
    private static List<RespuestaRevelada> revelarRespuestas(Partida partida, Ronda ronda, Jugador yo) {
        Fase fase = partida.fase();
        if (fase == Fase.RESPUESTAS) {
            return List.of();
        }
        boolean mostrarVotos = fase == Fase.DECISION || fase == Fase.RESULTADO || fase == Fase.TERMINADA;
        boolean mostrarAutor = fase == Fase.RESULTADO || fase == Fase.TERMINADA;
        String yoId = (yo == null) ? null : yo.id();
        return ronda.respuestas().stream()
                .map(r -> new RespuestaRevelada(
                        r.id(),
                        r.cartas().stream().map(CartaBlanca::texto).toList(),
                        frase(ronda.cartaNegra(), r.cartas()),
                        r.autorId().equals(yoId),
                        mostrarVotos ? ronda.votosDe(r.id()) : null,
                        mostrarAutor ? r.autorId() : null))
                .toList();
    }

    /** Completa la carta negra reemplazando cada ____ por el texto de las cartas jugadas, en orden. */
    private static String frase(CartaNegra negra, List<CartaBlanca> cartas) {
        String texto = negra.texto();
        for (CartaBlanca carta : cartas) {
            texto = texto.replaceFirst("____", Matcher.quoteReplacement(carta.texto()));
        }
        return texto;
    }
}
