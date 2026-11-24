// src/components/CallPanel.tsx
//
// Panel visible en la interfaz con cada llamada: método, URL, código y tiempo.
// Sirve para "demostrar" las llamadas en la defensa sin depender solo de la
// pestaña Network. Usa useSyncExternalStore para leer el store sin re-renders de más.

import { useState, useSyncExternalStore } from "react";
import { subscribe, getCalls, clearCalls, type CallRecord } from "../api/callLog";

function statusClass(c: CallRecord): string {
  if (c.status === 0 || c.status >= 500) return "call-status-error";
  if (
    c.status >= 400 ||
    (c.method === "HEAD" && c.url.endsWith("/api/salud") && c.status !== 200)
  ) {
    return "call-status-warning";
  }
  return "call-status-success";
}

export function CallPanel() {
  const calls = useSyncExternalStore(subscribe, getCalls);
  // Visible por defecto: el enunciado pide mostrar las llamadas en la interfaz.
  const [abierto, setAbierto] = useState(true);

  return (
    <section
      aria-label="Registro de llamadas"
      className="call-panel"
    >
      <header className="call-panel-header">
        <strong>Llamadas ({calls.length})</strong>
        <div className="call-panel-acciones">
          <button
            type="button"
            onClick={() => setAbierto((a) => !a)}
            aria-expanded={abierto}
            aria-controls="call-panel-contenido"
            className="btn-secundario call-panel-clear"
          >
            {abierto ? "Ocultar" : "Mostrar"}
          </button>
          <button type="button" onClick={clearCalls} className="btn-secundario call-panel-clear">
            Limpiar
          </button>
        </div>
      </header>

      {!abierto ? null : calls.length === 0 ? (
        <p id="call-panel-contenido" className="call-panel-empty">Aún no se han hecho llamadas.</p>
      ) : (
        <div id="call-panel-contenido" className="call-panel-table-wrap">
          <table className="call-panel-table">
            <thead>
              <tr>
                <th scope="col">Hora</th>
                <th scope="col">Método</th>
                <th scope="col">Código</th>
                <th scope="col">ms</th>
                <th scope="col">URL</th>
              </tr>
            </thead>
            <tbody>
              {calls.map((c) => (
                <tr key={c.id}>
                  <td>{c.at}</td>
                  <td>{c.method}</td>
                  <td className={statusClass(c)}>{c.status === 0 ? "—" : c.status}</td>
                  <td>{c.ms}</td>
                  <td className="call-panel-url">{c.url}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default CallPanel;
