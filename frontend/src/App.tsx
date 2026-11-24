// src/App.tsx
//
// Router delgado (persona A). Decide qué pantalla mostrar según el estado:
//   sin sesión  -> Configuración -> Entrada (crear/unirse)
//   con partida -> Ronda (pantalla de juego, persona B)
// El panel de llamadas va siempre visible abajo.

import { useEffect, useState } from "react";
import { Configuracion, type ConnectionStatus } from "./screens/Configuracion";
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
  const [connectionStatus, setConnectionStatus] =
    useState<ConnectionStatus>("disconnected");
  const [healthError, setHealthError] = useState<string | null>(null);

  // Polling mientras haya una partida activa.
  useEffect(() => {
    if (partidaId) {
      iniciarPolling(2000);
      return () => detenerPolling();
    }
  }, [partidaId]);

  return (
    <main className="app-shell">
      {partida ? (
        <Ronda />
      ) : cfg ? (
        /* Se pasa la prop onBack para permitir limpiar el estado de configuración y volver atrás */
        <Entrada
          cfg={cfg}
          connectionStatus={connectionStatus}
          onBack={() => {
            setCfg(null);
            setConnectionStatus("disconnected");
            setHealthError(null);
          }}
        />
      ) : (
        <Configuracion
          connectionStatus={connectionStatus}
          healthError={healthError}
          onBackendChange={() => {
            setConnectionStatus("disconnected");
            setHealthError(null);
          }}
          onReady={(data) => {
            setConnectionStatus("checking");
            setHealthError(null);
            return probarSalud()
              .then(() => {
                setConnectionStatus("verified");
                setCfg(data);
              })
              .catch((error: unknown) => {
                console.error("No se pudo verificar la conexión con el backend:", error);
                setConnectionStatus("disconnected");
                setHealthError(
                  "No se pudo verificar el servidor. Comprueba la URL y que el health check responda HTTP 200.",
                );
              });
          }}
        />
      )}

      <hr className="app-divider" />
      <CallPanel />
    </main>
  );
}