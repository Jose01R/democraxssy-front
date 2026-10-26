// src/screens/Ronda.tsx
//
// Pantalla principal del juego (responsabilidad de la persona B).
// Es UNA sola pantalla que cambia según partida.fase. Lee todo del store y
// dispara las acciones del store; no toca el cliente generado.
//
// SEGURIDAD XSS: nombres, frases y cartas pueden traer <script> a propósito.
// Se muestran SIEMPRE como texto con {}. React escapa automáticamente.
// NUNCA usar dangerouslySetInnerHTML aquí.

import { useState } from "react";
import {
  usePartida,
  enviarRespuesta,
  emitirVoto,
  elegirGanadora,
  avanzarRonda,
  comenzar,
  eliminar,
} from "../state/partida";
import { Cenicero, JarraCerveza, PilaRespuestas, TazaCafe } from "../components/DecoracionMesa";

// Divide la carta negra en texto y espacios en blanco (marcados con "____").
// Cada espacio se llena, en orden, con las cartas elegidas. Todo se pinta
// como texto: nunca se interpreta HTML de la API.
function CartaPregunta({
  texto,
  elegir,
  rellenos,
  pie,
}: {
  texto: string;
  elegir: number;
  rellenos: string[];
  pie: string;
}) {
  const partes = texto.split(/_{2,}/);
  const sinEspacios = partes.length === 1;
  // Ajusta la carta blanca al hueco: minúscula a mitad de frase y sin el
  // punto final si la carta negra sigue después del espacio.
  const ajustar = (relleno: string, i: number) => {
    let r = relleno.trim();
    const antes = partes[i].trimEnd();
    if (antes !== "" && !/[.!?¿¡:]$/.test(antes) && /^\p{Lu}\p{Ll}/u.test(r)) r = r.charAt(0).toLowerCase() + r.slice(1);
    if ((partes[i + 1] ?? "").trim() !== "") r = r.replace(/[.!?]+$/, "");
    return r;
  };
  return (
    <div className="carta-pregunta">
      <div className="carta-pregunta-header">
        <span className="carta-pregunta-tipo">
          <span className="material-symbols-outlined" aria-hidden="true">verified_user</span>
          Carta pregunta
        </span>
        <span className="carta-pregunta-elige">Elige {elegir}</span>
      </div>
      <p className="carta-pregunta-texto">
        {partes.map((parte, i) => {
          // La puntuación que sigue al espacio se pega a él para que no
          // quede sola en la línea siguiente.
          const texto = i > 0 ? parte.replace(/^[.,;:!?]+/, "") : parte;
          const pegada = partes[i + 1]?.match(/^[.,;:!?]+/)?.[0] ?? "";
          return (
            <span key={i}>
              {texto}
              {i < partes.length - 1 && (
                <span className="espacio-grupo">
                  {rellenos[i] ? (
                    <span className="espacio-lleno">{ajustar(rellenos[i], i)}</span>
                  ) : (
                    <span className="espacio-vacio" aria-label="espacio en blanco" />
                  )}
                  {pegada}
                </span>
              )}
            </span>
          );
        })}
        {sinEspacios && rellenos.length > 0 && (
          <span className="espacio-lleno espacio-aparte">{rellenos.join(" / ")}</span>
        )}
      </p>
      <div className="carta-pregunta-pie">
        <span>Cards for democraXSSy</span>
        <span>{pie}</span>
      </div>
    </div>
  );
}

export function Ronda() {
  const { partida, jugadorId, error, cargando } = usePartida();
  const [seleccion, setSeleccion] = useState<number[]>([]);
  // La API no devuelve mi respuesta hasta la votación: la recordamos aquí
  // para seguir mostrándola en la carta negra después de bajarla.
  const [enviada, setEnviada] = useState<{ ronda: number; textos: string[] } | null>(null);
  const [estadoCopia, setEstadoCopia] = useState<"idle" | "copiado" | "error">("idle");

  if (!partida) return <p>Cargando partida…</p>;

  const { fase, jugadores, ronda, yo, anfitrion_id, ganador_id, historial } = partida;
  const soyAnfitrion = jugadorId === anfitrion_id;
  const soySoberano = ronda != null && jugadorId === ronda.soberano_id;
  
  const nombrePorId = (id: string | null) =>
    jugadores.find((j) => j.id === id)?.nombre ?? "—";

  const ganadora = ronda?.respuestas.find((r) => r.id === ronda.ganadora);
  const respetoAlPueblo = historial?.find((h) => h.numero === ronda?.numero)?.respeto_al_pueblo;

  const ultimaRonda = historial.length > 0 ? historial[historial.length - 1] : null;

  const yaRespondi = jugadores.find((j) => j.id === jugadorId)?.ya_respondio ?? false;
  const textoDeCarta = (id: number) => yo?.mano.find((c) => c.id === id)?.texto ?? "";
  // Qué llena los espacios de la carta negra: lo que elijo, lo que bajé
  // o, en el resultado, las cartas de la ganadora.
  const rellenos = !ronda
    ? []
    : fase === "respuestas"
      ? enviada?.ronda === ronda.numero
        ? enviada.textos
        : seleccion.map(textoDeCarta)
      : fase === "resultado"
        ? (ganadora?.cartas ?? [])
        : [];

  const votantes = jugadores.length - 1;
  const estadoMesa = !ronda
    ? null
    : fase === "respuestas"
      ? {
          cuenta: `${ronda.respuestas_recibidas} / ${votantes} cartas en la mesa`,
          detalle: soySoberano
            ? "Esperando las respuestas del pueblo"
            : yaRespondi
              ? "Tu carta ya está en la mesa"
              : "Esperando que bajes tu carta",
        }
      : fase === "votacion"
        ? { cuenta: `${ronda.votos_emitidos} / ${votantes} votos`, detalle: "El pueblo está votando" }
        : fase === "decision"
          ? { cuenta: "Decisión", detalle: `${nombrePorId(ronda.soberano_id)} elige la ganadora` }
          : { cuenta: "Ronda resuelta", detalle: "Revisa el resultado" };

  async function bajarCartas() {
    if (!ronda) return;
    const textos = seleccion.map(textoDeCarta);
    try {
      await enviarRespuesta(seleccion);
      setEnviada({ ronda: ronda.numero, textos });
      setSeleccion([]);
    } catch {
      // El mensaje ya quedó en el store y se muestra arriba.
    }
  }

  // Si ya hay tantas cartas como pide la carta negra, la nueva reemplaza
  // a la que se eligió primero (con elegir = 1, simplemente cambia de carta).
  function toggleCarta(id: number, elegir: number) {
    setSeleccion((prev) =>
      prev.includes(id)
        ? prev.filter((x) => x !== id)
        : prev.length < elegir
          ? [...prev, id]
          : [...prev.slice(prev.length - elegir + 1), id],
    );
  }

  const copiarId = async () => {
    try {
      await navigator.clipboard.writeText(partida.id);
      setEstadoCopia("copiado");
      setTimeout(() => setEstadoCopia("idle"), 2500);
    } catch (err) {
      console.error("Error al copiar el ID: ", err);
      setEstadoCopia("error");
      setTimeout(() => setEstadoCopia("idle"), 3000);
    }
  };

  return (
    <section>
      {/* Cabecera común: jugadores y puntos (los nombres se muestran como texto) */}
      <div className="barra-mesa">
        <ul className="barra-jugadores" aria-label="Jugadores">
          {jugadores.map((j) => {
            const soyYo = j.id === jugadorId;
            const clases = ["chip-jugador"];
            if (j.es_soberano) clases.push("es-soberano");
            if (soyYo) clases.push("es-yo");
            return (
              <li key={j.id} className={clases.join(" ")}>
                <span className="chip-avatar" aria-hidden="true">
                  {Array.from(j.nombre)[0]?.toUpperCase() ?? "?"}
                  {j.es_soberano && (
                    <span className="material-symbols-outlined chip-corona">crown</span>
                  )}
                </span>
                <span className="chip-texto">
                  {j.es_soberano && <span className="chip-rol">Soberano</span>}
                  <span className="chip-nombre">{soyYo ? `${j.nombre} (tú)` : j.nombre}</span>
                </span>
                <span className="chip-puntos">
                  {j.puntos} {j.puntos === 1 ? "pt" : "pts"}
                </span>
                {j.es_anfitrion && (
                  <span className="material-symbols-outlined chip-estado" title="Anfitrión">
                    home
                  </span>
                )}
                {(j.ya_respondio || j.ya_voto) && (
                  <span
                    className="material-symbols-outlined chip-estado chip-listo"
                    title={j.ya_voto ? "Ya votó" : "Ya respondió"}
                  >
                    check_circle
                  </span>
                )}
              </li>
            );
          })}
        </ul>

        <div className="barra-info">
          {ronda && fase !== "sala" && (
            <span className="info-pill">Ronda {ronda.numero}</span>
          )}
          <span className="info-pill">Meta: {partida.puntos_para_ganar} pts</span>
        </div>
      </div>

      {error && <p className="msj-error">{error}</p>}

      {/* ===== sala ===== */}
      {fase === "sala" && (
        <div className="panel-mesa panel-sala">
          <span className="panel-kicker">Sala de espera</span>
          <h2>Mesa de espera</h2>
          
          <div className="id-box">
            <div>
              <span>ID de la Partida</span>
              <strong>{partida.id}</strong>
            </div>
            
            <div className="copiar-wrapper">
              {estadoCopia === "copiado" && <span className="msg-exito">¡Copiado!</span>}
              {estadoCopia === "error" && <span className="msg-error">Fallo al copiar</span>}
              <button type="button" onClick={copiarId} className="btn-secundario">
                Copiar ID
              </button>
            </div>
          </div>

          <div className="sala-conteo">
            <p>Jugadores en la mesa: {jugadores.length}</p>
            {jugadores.length < 4 ? (
              <p className="alerta-falta">
                Faltan {4 - jugadores.length} jugador(es) para poder iniciar.
              </p>
            ) : jugadores.length % 2 !== 0 ? (
              <p className="alerta-par">
                Falta 1 jugador (deben ser un número par).
              </p>
            ) : (
              <p className="alerta-ok">
                ¡Mesa lista para repartir!
              </p>
            )}
          </div>

          {soyAnfitrion ? (
            <button
              type="button"
              className="btn-primario btn-ancho"
              onClick={() => void comenzar()}
              disabled={cargando || jugadores.length < 4 || jugadores.length % 2 !== 0}
            >
              Iniciar partida
            </button>
          ) : (
            <div className="mensaje-espera">
              Esperando a que el anfitrión reparta las cartas...
            </div>
          )}
        </div>
      )}

      {/* ===== mesa: carta negra, mazo y estado de la ronda ===== */}
      {ronda && estadoMesa && fase !== "sala" && fase !== "terminada" && (
        <div className="mesa">
          <div className="mesa-lado mesa-lado-izq">
            <Cenicero />
            <TazaCafe />
          </div>

          <CartaPregunta
            texto={ronda.carta_negra.texto}
            elegir={ronda.carta_negra.elegir}
            rellenos={rellenos}
            pie={`Ronda ${ronda.numero}`}
          />

          <div className="mesa-estado">
            <PilaRespuestas
              cantidad={fase === "respuestas" ? ronda.respuestas_recibidas : ronda.respuestas.length}
            />
            <strong>{estadoMesa.cuenta}</strong>
            <span>{estadoMesa.detalle}</span>
          </div>

          <div className="mesa-lado mesa-lado-der">
            <JarraCerveza />
          </div>
        </div>
      )}

      {/* ===== respuestas: elegir cartas de la mano ===== */}
      {fase === "respuestas" && ronda && (
        soySoberano ? (
          <div className="panel-mesa espera-soberano">
            <span className="material-symbols-outlined espera-soberano-icon" aria-hidden="true">
              workspace_premium
            </span>
            <h2>Eres el soberano de esta ronda.</h2>
            <p>
              Espera a que la mesa baje sus cartas ({ronda.respuestas_recibidas} recibidas).
            </p>
          </div>
        ) : yaRespondi ? (
          <div className="panel-mesa espera-soberano">
            <span className="material-symbols-outlined espera-soberano-icon" aria-hidden="true">
              hourglass_top
            </span>
            <h2>Tu respuesta ya está en la mesa.</h2>
            <p>Espera a que los demás bajen sus cartas.</p>
          </div>
        ) : (
          <div className="fase-cartas">
            <div className="encabezado-mano">
              <span className="material-symbols-outlined" aria-hidden="true">style</span>
              <h3>
                Tu mano de respuestas{" "}
                <span>(Elige {ronda.carta_negra.elegir})</span>
              </h3>
            </div>

            <div className="contenedor-mano">
              {yo?.mano.map((c) => {
                const seleccionada = seleccion.includes(c.id);
                return (
                  <button
                    key={c.id}
                    type="button"
                    aria-pressed={seleccionada}
                    onClick={() => toggleCarta(c.id, ronda.carta_negra.elegir)}
                    className="carta-fisica"
                  >
                    {seleccionada && (
                      <span className="badge-seleccion">
                        <span className="material-symbols-outlined" aria-hidden="true">
                          check_circle
                        </span>
                        Seleccionada
                      </span>
                    )}
                    <span className="carta-header">Respuesta</span>
                    <span className="carta-cuerpo">{c.texto}</span>
                    <span className="carta-footer">DEMOCRAXSSY</span>
                  </button>
                );
              })}
            </div>

            <div className="acciones-mano">
              <button
                type="button"
                className="btn-primario boton-bajar-carta"
                disabled={cargando || seleccion.length !== ronda.carta_negra.elegir}
                onClick={() => void bajarCartas()}
              >
                <span className="material-symbols-outlined" aria-hidden="true">
                  move_down
                </span>
                {ronda.carta_negra.elegir > 1
                  ? "Bajar cartas a la mesa"
                  : "Bajar carta a la mesa"}
              </button>
            </div>
          </div>
        )
      )}

      {/* ===== votacion: votar una respuesta ajena ===== */}
      {fase === "votacion" && ronda && (
        <div className="fase-lista">
          <h3>
            {soySoberano ? "Espera a que el pueblo vote las opciones." : "Vota por tu respuesta favorita:"}
          </h3>
          <div className="lista-respuestas">
            {ronda.respuestas.map((r) => (
              <div key={r.id} className="respuesta-row">
                <span className="respuesta-texto">{r.frase}</span>
                {r.es_mia && <span className="etiqueta-mia">Tu respuesta</span>}
                {!soySoberano && !r.es_mia && (
                  <button
                    type="button"
                    className="btn-votar"
                    disabled={cargando || yo?.mi_voto != null}
                    onClick={() => void emitirVoto(r.id)}
                  >
                    {yo?.mi_voto === r.id ? "Votada" : "Votar"}
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ===== decision: el soberano elige la ganadora ===== */}
      {fase === "decision" && ronda && (
        <div className="fase-lista">
          <h3>
            {soySoberano ? "Elige la respuesta ganadora de esta ronda:" : "El soberano está decidiendo…"}
          </h3>
          <div className="lista-respuestas">
            {ronda.respuestas.map((r) => (
              <div key={r.id} className="respuesta-row">
                <span className="respuesta-texto">{r.frase}</span>
                <span className="votos-badge">
                  {r.votos ?? 0} votos
                </span>
                {soySoberano && (
                  <button 
                    type="button"
                    className="btn-elegir"
                    disabled={cargando} 
                    onClick={() => void elegirGanadora(r.id)}
                  >
                    Elegir Ganadora
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ===== resultado ===== */}
      {fase === "resultado" && ronda && (
        <div className="panel-mesa panel-resultado">
          <span className="panel-kicker">Ronda {ronda.numero} · Resultado</span>
          {ganadora ? (
            <>
              <div className="carta-ganadora">
                <span className="carta-ganadora-cinta">
                  <span className="material-symbols-outlined" aria-hidden="true">emoji_events</span>
                  Respuesta ganadora
                </span>
                <p className="carta-ganadora-frase">{ganadora.frase}</p>
                <span className="carta-ganadora-autor">
                  Bajada por <strong>{nombrePorId(ganadora.autor_id ?? null)}</strong>
                  {" · "}
                  {ganadora.votos ?? 0} {ganadora.votos === 1 ? "voto" : "votos"} del pueblo
                </span>
              </div>

              {respetoAlPueblo !== undefined && (
                <div className={`respeto-box ${respetoAlPueblo ? "si" : "no"}`}>
                  <span className="material-symbols-outlined" aria-hidden="true">
                    {respetoAlPueblo ? "handshake" : "gavel"}
                  </span>
                  <div>
                    <strong>
                      {respetoAlPueblo ? "El soberano escuchó al pueblo" : "El soberano ignoró al pueblo"}
                    </strong>
                    <p>
                      {respetoAlPueblo
                        ? "La ganadora también fue de las más votadas por la mesa."
                        : "Eligió a su criterio, contra la mayoría de los votos."}
                    </p>
                  </div>
                </div>
              )}
            </>
          ) : (
            <p>Ronda resuelta.</p>
          )}

          {yo?.puede_avanzar ? (
            <button
              type="button"
              className="btn-primario boton-bajar-carta"
              disabled={cargando}
              onClick={() => void avanzarRonda()}
            >
              <span className="material-symbols-outlined" aria-hidden="true">skip_next</span>
              Siguiente ronda
            </button>
          ) : (
            <p className="panel-nota">El anfitrión o el soberano pasarán a la siguiente ronda.</p>
          )}
        </div>
      )}

      {/* ===== terminada ===== */}
      {fase === "terminada" && (
        <div className="panel-mesa panel-terminada">
          <span className="material-symbols-outlined trofeo" aria-hidden="true">trophy</span>
          <span className="panel-kicker">Fin de la partida</span>
          <h2>Ganó {nombrePorId(ganador_id)}</h2>

          {ultimaRonda && (
            <div className="carta-ganadora">
              <span className="carta-ganadora-cinta">
                <span className="material-symbols-outlined" aria-hidden="true">emoji_events</span>
                Última frase ganadora
              </span>
              <p className="carta-ganadora-frase">{ultimaRonda.frase}</p>
              <span className="carta-ganadora-autor">
                Ronda {ultimaRonda.numero} · eligió {nombrePorId(ultimaRonda.soberano_id)}
              </span>
            </div>
          )}

          <ol className="tabla-posiciones">
            {[...jugadores]
              .sort((a, b) => b.puntos - a.puntos)
              .map((j, index) => (
                <li key={j.id} className={j.id === ganador_id ? "es-ganador" : undefined}>
                  <span className="posicion">{index + 1}</span>
                  <span className="posicion-nombre">{j.nombre}</span>
                  <span className="chip-puntos">
                    {j.puntos} {j.puntos === 1 ? "pt" : "pts"}
                  </span>
                </li>
              ))}
          </ol>

          {soyAnfitrion ? (
            <button
              type="button"
              className="btn-peligro"
              disabled={cargando}
              onClick={() => void eliminar()}
            >
              Eliminar partida
            </button>
          ) : (
            <p className="panel-nota">El anfitrión puede eliminar la partida.</p>
          )}
        </div>
      )}
    </section>
  );
}

export default Ronda;