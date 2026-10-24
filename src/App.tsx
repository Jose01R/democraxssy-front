// src/App.tsx
//
// Router delgado (persona A). Decide qué pantalla mostrar según el estado:
//   sin sesión  -> Configuración -> Entrada (crear/unirse)
//   con partida -> Ronda (pantalla de juego, persona B)
// El panel de llamadas va siempre visible abajo.

import { useEffect, useState } from "react";
import { Configuracion } from "./screens/Configuracion";
import { Ronda } from "./screens/Ronda";
import { Entrada } from "./screens/Entrada";
import { CallPanel } from "./components/CallPanel";
import type { ConfigForm } from "./schemas/config";
import {
  usePartida,
  probarSalud,
  iniciarPolling,
  detenerPolling,
} from "./state/partida";

export default function App() {
  const { partida, partidaId } = usePartida();
  const [cfg, setCfg] = useState<ConfigForm | null>(null);

  // Polling mientras haya una partida activa.
  useEffect(() => {
    if (partidaId) {
      iniciarPolling(2000);
      return () => detenerPolling();
    }
  }, [partidaId]);

  return (
    <main style={{ maxWidth: 820, margin: "0 auto", padding: "1.5rem" }}>
      <h1>Cards for democraXSSy</h1>

      {partida ? (
        <Ronda />
      ) : cfg ? (
        /* Se pasa la prop onBack para permitir limpiar el estado de configuración y volver atrás */
        <Entrada cfg={cfg} onBack={() => setCfg(null)} />
      ) : (
        <Configuracion
          onReady={(data) => {
            setCfg(data);
            void probarSalud(); // health check: primera fila en el panel
          }}
        />
      )}

      <hr style={{ margin: "1.5rem 0", opacity: 0.3 }} />
      <CallPanel />
    </main>
  );
}