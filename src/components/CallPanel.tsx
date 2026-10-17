// src/components/CallPanel.tsx
//
// Panel visible en la interfaz con cada llamada: método, URL, código y tiempo.
// Sirve para "demostrar" las llamadas en la defensa sin depender solo de la
// pestaña Network. Usa useSyncExternalStore para leer el store sin re-renders de más.

import { useSyncExternalStore } from "react";
import { subscribe, getCalls, clearCalls, type CallRecord } from "../api/callLog";

function statusColor(c: CallRecord): string {
  if (c.status === 0) return "#b4232a"; // sin respuesta (red/CORS)
  if (c.status >= 500) return "#b4232a";
  if (c.status === 409) return "#b06a00"; // el 409 que nos interesa vigilar
  if (c.status >= 400) return "#b06a00";
  return "#1f7a3d"; // 2xx/3xx
}

export function CallPanel() {
  const calls = useSyncExternalStore(subscribe, getCalls);

  return (
    <section
      aria-label="Registro de llamadas"
      style={{
        border: "1px solid currentColor",
        borderRadius: 6,
        padding: "0.5rem 0.75rem",
        fontSize: 13,
        opacity: 0.95,
      }}
    >
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 6,
        }}
      >
        <strong>Llamadas ({calls.length})</strong>
        <button type="button" onClick={clearCalls} style={{ fontSize: 12 }}>
          Limpiar
        </button>
      </header>

      {calls.length === 0 ? (
        <p style={{ margin: 0, opacity: 0.6 }}>Aún no se han hecho llamadas.</p>
      ) : (
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
          }}
        >
          <thead>
            <tr style={{ textAlign: "left", opacity: 0.7 }}>
              <th style={{ padding: "2px 6px" }}>Hora</th>
              <th style={{ padding: "2px 6px" }}>Método</th>
              <th style={{ padding: "2px 6px" }}>Código</th>
              <th style={{ padding: "2px 6px" }}>ms</th>
              <th style={{ padding: "2px 6px" }}>URL</th>
            </tr>
          </thead>
          <tbody>
            {calls.map((c) => (
              <tr key={c.id} style={{ borderTop: "1px solid rgba(128,128,128,0.3)" }}>
                <td style={{ padding: "2px 6px" }}>{c.at}</td>
                <td style={{ padding: "2px 6px" }}>{c.method}</td>
                <td style={{ padding: "2px 6px", color: statusColor(c), fontWeight: 600 }}>
                  {c.status === 0 ? "—" : c.status}
                </td>
                <td style={{ padding: "2px 6px" }}>{c.ms}</td>
                <td style={{ padding: "2px 6px", wordBreak: "break-all" }}>{c.url}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

export default CallPanel;
