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

  if (!partida) return <p>Cargando partida…</p>;

  const { fase, jugadores, ronda, yo, anfitrion_id, ganador_id } = partida;
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

  return (
    <section>
      {/* Cabecera común: jugadores y puntos */}
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 12 }}>
        {jugadores.map((j) => (
          <span key={j.id} style={{ border: "1px solid currentColor", borderRadius: 4, padding: "2px 8px" }}>
            {j.nombre} · {j.puntos} pts
            {j.es_soberano ? " 👑" : ""}
            {j.ya_respondio ? " ✓" : ""}
          </span>
        ))}
      </div>

      {error && <p style={{ color: "#b4232a" }}>{error}</p>}

      {/* ===== sala ===== */}
      {fase === "sala" && (
        <div>
          <p>Partida <code>{partida.id}</code> — comparte este ID para que se unan.</p>
          <p>Jugadores: {jugadores.length} (se necesitan mínimo 4 en número par).</p>
          {soyAnfitrion && (
            <button
              onClick={() => void comenzar()}
              disabled={cargando || jugadores.length < 4 || jugadores.length % 2 !== 0}
            >
              Iniciar partida
            </button>
          )}
        </div>
      )}

      {/* ===== carta negra (visible en todas las fases de ronda) ===== */}
      {ronda && fase !== "sala" && fase !== "terminada" && (
        <blockquote style={{ borderLeft: "3px solid currentColor", paddingLeft: 12 }}>
          Ronda {ronda.numero} · soberano: {nombrePorId(ronda.soberano_id)}
          <br />
          <strong>{ronda.carta_negra.texto}</strong> (elegir {ronda.carta_negra.elegir})
        </blockquote>
      )}

      {/* ===== respuestas: elegir cartas de la mano ===== */}
      {fase === "respuestas" && ronda && (
        soySoberano ? (
          <p>Eres el soberano: espera a que respondan ({ronda.respuestas_recibidas} recibidas).</p>
        ) : (
          <div>
            <p>Elige {ronda.carta_negra.elegir} carta(s) de tu mano:</p>
            <div style={{ display: "grid", gap: 6 }}>
              {yo?.mano.map((c) => (
                <label key={c.id} style={{ display: "flex", gap: 8 }}>
                  <input
                    type="checkbox"
                    checked={seleccion.includes(c.id)}
                    onChange={() => toggleCarta(c.id, ronda.carta_negra.elegir)}
                  />
                  {c.texto}
                </label>
              ))}
            </div>
            <button
              style={{ marginTop: 8 }}
              disabled={cargando || seleccion.length !== ronda.carta_negra.elegir}
              onClick={() => {
                void enviarRespuesta(seleccion);
                setSeleccion([]);
              }}
            >
              Enviar respuesta
            </button>
          </div>
        )
      )}

      {/* ===== votacion: votar una respuesta ajena ===== */}
      {fase === "votacion" && ronda && (
        <div>
          <p>{soySoberano ? "Espera a que el pueblo vote." : "Vota tu favorita:"}</p>
          <div style={{ display: "grid", gap: 6 }}>
            {ronda.respuestas.map((r) => (
              <div key={r.id} style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <span style={{ flex: 1 }}>{r.frase}</span>
                {!soySoberano && !r.es_mia && (
                  <button
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
        <div>
          <p>{soySoberano ? "Elige la respuesta ganadora:" : "El soberano está decidiendo…"}</p>
          <div style={{ display: "grid", gap: 6 }}>
            {ronda.respuestas.map((r) => (
              <div key={r.id} style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <span style={{ flex: 1 }}>{r.frase}</span>
                <span>{r.votos ?? 0} votos</span>
                {soySoberano && (
                  <button disabled={cargando} onClick={() => void elegirGanadora(r.id)}>
                    Elegir
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ===== resultado: ganadora de la ronda + avanzar ===== */}
      {fase === "resultado" && ronda && (
        <div>
          {(() => {
            const g = ronda.respuestas.find((r) => r.id === ronda.ganadora);
            return g ? (
              <p>Ganó: <strong>{g.frase}</strong> — de {nombrePorId(g.autor_id ?? null)}</p>
            ) : (
              <p>Ronda resuelta.</p>
            );
          })()}
          {yo?.puede_avanzar && (
            <button disabled={cargando} onClick={() => void avanzarRonda()}>
              Siguiente ronda
            </button>
          )}
        </div>
      )}

      {/* ===== terminada: ganador de la partida ===== */}
      {fase === "terminada" && (
        <div>
          <h2>Ganó {nombrePorId(ganador_id)} 🎉</h2>
          {soyAnfitrion && (
            <button disabled={cargando} onClick={() => void eliminar()}>
              Eliminar partida
            </button>
          )}
        </div>
      )}
    </section>
  );
}

export default Ronda;
