// src/components/IndicadorConexion.tsx
//
// Chip discreto que informa por qué vía viajan las llamadas al backend:
//   - https:// -> "Conexión directa (HTTPS)"  (CORS abierto)
//   - http://  -> "Conexión vía proxy (HTTP)" (contenido mixto, pasa por Nginx)
// El modo NO se calcula aquí: se reutiliza la misma regla de la API
// (getConnectionMode) para que el texto nunca contradiga las peticiones reales.
// Todo es texto plano: sin HTML inyectado ni recursos externos (CSP).

import { getConnectionMode } from "../api/client";

export function IndicadorConexion() {
  const modo = getConnectionMode();

  // Sin backend configurado aún no hay ninguna vía que anunciar.
  if (modo === "none") return null;

  const esDirecta = modo === "direct";
  const texto = esDirecta ? "Conexión directa (HTTPS)" : "Conexión vía proxy (HTTP)";

  return (
    <span
      className={`indicador-conexion ${esDirecta ? "es-directa" : "es-proxy"}`}
      title={
        esDirecta
          ? "Las llamadas van directamente al backend por HTTPS."
          : "Las llamadas van por el proxy del servidor (HTTP), por contenido mixto."
      }
    >
      <span className="material-symbols-outlined indicador-conexion-icono" aria-hidden="true">
        {esDirecta ? "lock" : "swap_horiz"}
      </span>
      {texto}
    </span>
  );
}

export default IndicadorConexion;
