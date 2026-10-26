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

export function Ronda() {
  const { partida, jugadorId, error, cargando } = usePartida();
  const [seleccion, setSeleccion] = useState<number[]>([]);
  const [estadoCopia, setEstadoCopia] = useState<"idle" | "copiado" | "error">("idle");

  if (!partida) return <p>Cargando partida…</p>;

  const { fase, jugadores, ronda, yo, anfitrion_id, ganador_id, historial } = partida;
  const soyAnfitrion = jugadorId === anfitrion_id;
  const soySoberano = ronda != null && jugadorId === ronda.soberano_id;
  
  const nombrePorId = (id: string | null) =>
    jugadores.find((j) => j.id === id)?.nombre ?? "—";

  function toggleCarta(id: number, elegir: number) {
    setSeleccion((prev) =>
      prev.includes(id)
        ? prev.filter((x) => x !== id)
        : prev.length < elegir
          ? [...prev, id]
          : prev,
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
      {/* Cabecera común: jugadores y puntos */}
      <div className="cabecera-jugadores">
        {jugadores.map((j) => (
          <span key={j.id} className="jugador-badge">
            {j.nombre} · {j.puntos} pts
            {j.es_soberano ? " 👑" : ""}
            {j.ya_respondio ? " ✓" : ""}
            {j.ya_voto ? " 🗳️" : ""}
          </span>
        ))}
      </div>

      {error && <p className="msj-error">{error}</p>}

      {/* ===== sala ===== */}
      {fase === "sala" && (
        <div className="panel-sala">
          <h2>Mesa de Espera</h2>
          
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

          <div style={{ marginBottom: "1.5rem" }}>
            <p style={{ fontWeight: "bold", margin: "0 0 0.25rem 0" }}>
              Jugadores en la mesa: {jugadores.length}
            </p>
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

      {/* ===== carta negra ===== */}
      {ronda && fase !== "sala" && fase !== "terminada" && (
        <blockquote className="carta-negra">
          <div className="carta-negra-header">
            Ronda {ronda.numero} · Soberano actual: <strong>{nombrePorId(ronda.soberano_id)}</strong>
          </div>
          <strong className="carta-negra-texto">{ronda.carta_negra.texto}</strong> 
          <span className="carta-negra-badge">
            (Elegir {ronda.carta_negra.elegir})
          </span>
        </blockquote>
      )}

      {/* ===== respuestas: elegir cartas de la mano ===== */}
      {fase === "respuestas" && ronda && (
        soySoberano ? (
          <div className="panel-resultado espera-soberano">
            <span className="material-symbols-outlined espera-soberano-icon" aria-hidden="true">
              workspace_premium
            </span>
            <h2>Eres el soberano de esta ronda.</h2>
            <p>
              Espera a que la mesa baje sus cartas ({ronda.respuestas_recibidas} recibidas).
            </p>
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
                    <span className="carta-footer">
                      #{String(c.id).padStart(2, "0")}
                    </span>
                  </button>
                );
              })}
            </div>

            <div className="acciones-mano">
              <button
                type="button"
                className="btn-primario boton-bajar-carta"
                disabled={cargando || seleccion.length !== ronda.carta_negra.elegir}
                onClick={() => {
                  void enviarRespuesta(seleccion);
                  setSeleccion([]);
                }}
              >
                <span className="material-symbols-outlined" aria-hidden="true">
                  move_down
                </span>
                Bajar carta a la mesa
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
                {!soySoberano && !r.es_mia && (
                  <button
                    type="button"
                    className="btn-votar"
                    disabled={cargando || yo?.mi_voto === r.id}
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
      {/* ===== resultado ===== */}
      {fase === "resultado" && ronda && (
        <div className="panel-resultado">
          {(() => {
            const g = ronda.respuestas.find((r) => r.id === ronda.ganadora);
            
            // Corrección: usamos h.numero en lugar de h.ronda
            const historialRonda = historial?.find((h) => h.numero === ronda.numero);
            const respetoAlPueblo = historialRonda?.respeto_al_pueblo;

            return g ? (
              <div>
                <p style={{ fontSize: "1.2rem", margin: "0 0 0.5rem 0" }}>Ganó: <strong>{g.frase}</strong></p>
                <p style={{ color: "#67574b", margin: 0 }}>— Autor: {nombrePorId(g.autor_id ?? null)}</p>
                
                {respetoAlPueblo !== undefined && (
                  <div className={`respeto-box ${respetoAlPueblo ? 'si' : 'no'}`}>
                    <strong style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                      {respetoAlPueblo ? "👑🤝 El Soberano escuchó al pueblo" : "👑📉 El Soberano ignoró al pueblo"}
                    </strong>
                    <p style={{ margin: "4px 0 0 0", fontSize: "0.9rem" }}>
                      {respetoAlPueblo 
                        ? "La respuesta ganadora fue también la más votada por la mesa." 
                        : "El Soberano eligió a su propio criterio ignorando la mayoría de votos."}
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <p>Ronda resuelta.</p>
            );
          })()}
          
          {yo?.puede_avanzar && (
            <div style={{ marginTop: "1.5rem" }}>
              <button 
                type="button"
                className="btn-primario"
                disabled={cargando} 
                onClick={() => void avanzarRonda()}
              >
                Siguiente ronda
              </button>
            </div>
          )}
        </div>
      )}

      {/* ===== terminada ===== */}
      {fase === "terminada" && (
        <div className="panel-terminada">
          <h2 style={{ fontSize: "2rem", margin: "0 0 1rem 0", color: "#231a14" }}>
            🏆 ¡Ganó {nombrePorId(ganador_id)}!
          </h2>
          
          <div className="tabla-posiciones">
            <h3 style={{ marginTop: 0, borderBottom: "2px solid #ece4da", paddingBottom: "0.5rem", color: "#4d3d32" }}>
              Clasificación Final
            </h3>
            <ul>
              {[...jugadores].sort((a, b) => b.puntos - a.puntos).map((j, index) => (
                <li key={j.id}>
                  <span>{index + 1}. {j.nombre} {index === 0 && "🥇"}</span>
                  <span>{j.puntos} pts</span>
                </li>
              ))}
            </ul>
          </div>

          {soyAnfitrion && (
            <button 
              type="button"
              className="btn-peligro"
              disabled={cargando} 
              onClick={() => void eliminar()}
            >
              Eliminar partida
            </button>
          )}
        </div>
      )}
    </section>
  );
}

export default Ronda;