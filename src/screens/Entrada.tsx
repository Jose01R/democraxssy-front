// src/screens/Entrada.tsx

import { useState } from "react";
import type { ConfigForm } from "../schemas/config";
import { usePartida, crear, entrar } from "../state/partida";

type Props = {
  cfg: ConfigForm;
  onBack: () => void;
};

function registrarError(error: unknown) {
  console.error("No se pudo completar la acción de partida:", error);
}

export function Entrada({ cfg, onBack }: Props) {
  const { cargando, error } = usePartida();
  const [idUnirse, setIdUnirse] = useState("");

  // Validación exacta: exactamente 32 caracteres hexadecimales en minúscula (0-9, a-f)
  const regexHex32 = /^[0-9a-f]{32}$/;
  const idValido = regexHex32.test(idUnirse.trim());

  return (
    <div style={{ width: "100%", padding: "1rem", boxSizing: "border-box" }}>
      <div className="dossier-card">
        <div className="dossier-header">
          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <div
              aria-hidden="true"
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                backgroundColor: "#e8ded4",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#1d5452",
                flexShrink: 0,
              }}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 28 }}>
                style
              </span>
            </div>

            <div>
              <span
                className="mono-text"
                style={{
                  fontSize: "0.75rem",
                  fontWeight: 700,
                  color: "var(--gold-accent)",
                }}
              >
                PASO 2 DE 2
              </span>
              <h1 style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800 }}>
                Acceso a la mesa
              </h1>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", flexWrap: "wrap" }}>
            <button
              type="button"
              onClick={onBack}
              className="btn-secundario"
              style={{ fontSize: "0.8rem", padding: "0.4rem 0.75rem" }}
              title="Volver a la configuración inicial"
            >
              <span className="material-symbols-outlined" style={{ fontSize: "1rem", verticalAlign: "middle" }}>
                arrow_back
              </span>{" "}
              Cambiar servidor
            </button>

            <div
              className="mono-text"
              style={{
                padding: "0.35rem 0.75rem",
                backgroundColor: "#e8dbcc",
                color: "#2e7d7a",
                fontSize: "0.75rem",
                fontWeight: 700,
                borderRadius: 6,
                display: "flex",
                alignItems: "center",
                gap: "0.3rem",
              }}
            >
              <span className="material-symbols-outlined" aria-hidden="true" style={{ fontSize: 16 }}>
                person
              </span>
              <span>Jugador: {cfg.playerName || "Anónimo"}</span>
            </div>
          </div>
        </div>

        <div className="dossier-grid">
          {/* ANFITRIÓN */}
          <section
            style={{
              backgroundColor: "var(--bg-card-inner)",
              padding: "1.25rem",
              borderRadius: 12,
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-between",
              border: "1px solid var(--border-color)",
              gap: "1rem",
            }}
          >
            <div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "0.5rem",
                }}
              >
                <span
                  className="mono-text"
                  style={{
                    fontSize: "0.75rem",
                    fontWeight: 700,
                    color: "var(--teal-accent)",
                  }}
                >
                  ANFITRIÓN
                </span>
                <span
                  className="material-symbols-outlined"
                  aria-hidden="true"
                  style={{ color: "var(--teal-accent)" }}
                >
                  add_circle
                </span>
              </div>

              <h2 style={{ fontSize: "1.1rem", margin: "0 0 0.5rem" }}>
                Crear una partida
              </h2>
              <p
                style={{
                  fontSize: "0.8rem",
                  color: "var(--text-muted)",
                  lineHeight: 1.4,
                  marginBottom: "1rem",
                }}
              >
                Crea una sala nueva. La partida estará configurada para ganar con{" "}
                <strong>{cfg.pointsToWin} puntos</strong>.
              </p>
            </div>

            <button
              type="button"
              disabled={cargando}
              onClick={() =>
                void crear(cfg.playerName, cfg.pointsToWin).catch(registrarError)
              }
              className="btn-teal"
              style={{ width: "100%" }}
            >
              <span className="material-symbols-outlined" aria-hidden="true">
                add
              </span>
              <span>{cargando ? "Creando…" : "Crear partida nueva"}</span>
            </button>
          </section>

          {/* INVITADO */}
          <section
            style={{
              backgroundColor: "var(--bg-card-inner)",
              padding: "1.25rem",
              borderRadius: 12,
              display: "flex",
              flexDirection: "column",
              justifyContent: "space-between",
              border: "1px solid var(--border-color)",
              gap: "1rem",
            }}
          >
            <div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "0.5rem",
                }}
              >
                <span
                  className="mono-text"
                  style={{
                    fontSize: "0.75rem",
                    fontWeight: 700,
                    color: "var(--gold-accent)",
                  }}
                >
                  INVITADO
                </span>
                <span
                  className="material-symbols-outlined"
                  aria-hidden="true"
                  style={{ color: "var(--gold-accent)" }}
                >
                  vpn_key
                </span>
              </div>

              <h2 style={{ fontSize: "1.1rem", margin: "0 0 0.5rem" }}>
                Unirse con un ID
              </h2>
              <p
                id="id-partida-ayuda"
                style={{
                  fontSize: "0.8rem",
                  color: "var(--text-muted)",
                  lineHeight: 1.4,
                  marginBottom: "1rem",
                }}
              >
                Ingresa el ID de 32 caracteres de la partida que te compartieron.
              </p>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
              <label htmlFor="id-partida" className="input-label">
                ID de la partida
              </label>
              <input
                id="id-partida"
                type="text"
                autoComplete="off"
                placeholder="Ej. dcc7df3940fe2a53328..."
                value={idUnirse}
                onChange={(event) => setIdUnirse(event.target.value)}
                aria-describedby="id-partida-ayuda"
                className="input-field mono-text"
              />

              {/* Advertencia visual si el texto no cumple con el formato exacto de 32 hex */}
              {idUnirse.trim().length > 0 && !idValido && (
                <span style={{ fontSize: "0.75rem", color: "#b06a00" }}>
                  El ID debe tener exactamente 32 caracteres hexadecimales (0-9, a-f). ({idUnirse.trim().length}/32)
                </span>
              )}

              <button
                type="button"
                disabled={cargando || !idValido}
                onClick={() =>
                  void entrar(idUnirse.trim(), cfg.playerName).catch(registrarError)
                }
                className="btn-gold"
                style={{ width: "100%", opacity: !idValido ? 0.6 : 1 }}
              >
                <span className="material-symbols-outlined" aria-hidden="true">
                  login
                </span>
                <span>{cargando ? "Uniéndose…" : "Unirse a la partida"}</span>
              </button>
            </div>
          </section>
        </div>

        {error && (
          <div className="error-banner" role="alert" style={{ marginTop: "1rem" }}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}

export default Entrada;