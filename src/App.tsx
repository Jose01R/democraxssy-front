// src/App.tsx
//
// Router delgado (persona A). Decide qué pantalla mostrar según el estado:
//   sin sesión  -> Configuración -> Entrada (crear/unirse)
//   con partida -> Ronda (pantalla de juego, persona B)
// El panel de llamadas va siempre visible abajo.

import { useEffect, useState } from "react";
import { Configuracion } from "./screens/Configuracion";
import { Ronda } from "./screens/Ronda";
import { CallPanel } from "./components/CallPanel";
import type { ConfigForm } from "./schemas/config";
import {
  usePartida,
  probarSalud,
  crear,
  entrar,
  iniciarPolling,
  detenerPolling,
} from "./state/partida";

function Entrada({ cfg }: { cfg: ConfigForm }) {
  const { cargando, error } = usePartida();
  const [idUnirse, setIdUnirse] = useState("");

  return (
    <section style={{ display: "grid", gap: 16, maxWidth: 420 }}>
      <div>
        <button disabled={cargando} onClick={() => void crear(cfg.playerName, cfg.pointsToWin)}>
          Crear partida nueva
        </button>
      </div>
      <div style={{ display: "flex", gap: 8 }}>
        <input
          placeholder="ID de partida para unirse"
          value={idUnirse}
          onChange={(e) => setIdUnirse(e.target.value)}
        />
        <button disabled={cargando || !idUnirse} onClick={() => void entrar(idUnirse.trim(), cfg.playerName)}>
          Unirse
        </button>
      </div>
      {error && <p style={{ color: "#b4232a" }}>{error}</p>}
    </section>
  );
}

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
        <Entrada cfg={cfg} />
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
