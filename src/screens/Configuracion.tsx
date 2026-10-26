import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { configSchema, type ConfigForm } from "../schemas/config";
import { setBackend } from "../api/client";

export type ConnectionStatus = "disconnected" | "checking" | "verified";

type Props = {
  connectionStatus: ConnectionStatus;
  healthError: string | null;
  onBackendChange: () => void;
  onReady: (cfg: ConfigForm) => Promise<void>;
};

export function Configuracion({
  connectionStatus,
  healthError,
  onBackendChange,
  onReady,
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ConfigForm>({
    resolver: zodResolver(configSchema),
    defaultValues: {
      backendUrl: "https://democraxssy.akamai.meseguercr.com",
      playerName: "",
    },
  });

  const onSubmit = handleSubmit(async (data) => {
    setBackend(data.backendUrl);
    await onReady(data);
  });

  const statusLabel =
    connectionStatus === "verified"
      ? "Mesa Verificada"
      : connectionStatus === "checking"
        ? "Verificando conexión"
        : "Sin conectar";

  return (
    <div style={{ width: "100%", padding: "1rem", boxSizing: "border-box" }}>
      {/* Badge Superior */}
      <div style={{ textAlign: "center", marginBottom: "1.5rem" }}>
        <div 
          className="mono-text"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "0.5rem",
            backgroundColor: "#140d08",
            padding: "0.35rem 1rem",
            borderRadius: "20px",
            fontSize: "0.75rem",
            color: "#ecc15a",
            border: "1px solid rgba(255, 255, 255, 0.08)"
          }}
        >
          <span style={{ width: 8, height: 8, borderRadius: "50%", backgroundColor: "#ecc15a" }}></span>
          <span>CONFIGURACIÓN DE SERVIDOR • PROTOCOLO ARTESANAL</span>
        </div>
      </div>

      {/* Tarjeta Dossier */}
      <div className="dossier-card">
        {/* Header */}
        <div className="dossier-header">
          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <div style={{
              width: 48,
              height: 48,
              borderRadius: 12,
              backgroundColor: "#e8ded4",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#1d5452"
            }}>
              <span className="material-symbols-outlined" style={{ fontSize: 28 }}>settings</span>
            </div>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.2rem" }}>
                <span className="mono-text" style={{ fontSize: "0.7rem", fontWeight: 700, backgroundColor: "#e2d8ce", padding: "0.1rem 0.4rem", borderRadius: 4 }}>
                  PASO 1 DE 2
                </span>
                <span className="mono-text" style={{ fontSize: "0.75rem", color: "#7e6d61" }}>• Conexión al Salón</span>
              </div>
              <h1 style={{ margin: 0, fontSize: "1.75rem", fontWeight: 800 }}>Cards for democraXSSy</h1>
            </div>
          </div>

          <div
            className={`mono-text connection-status${connectionStatus === "verified" ? " is-verified" : ""}`}
            aria-live="polite"
          >
            <span className="material-symbols-outlined connection-status-icon">
              {connectionStatus === "verified"
                ? "verified"
                : connectionStatus === "checking"
                  ? "progress_activity"
                  : "cloud_off"}
            </span>
            <span>{statusLabel}</span>
          </div>
        </div>

        {/* Formulario */}
        <form onSubmit={onSubmit} style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
          <div className="dossier-grid">
            
            {/* Campo 1: Servidor Backend */}
            <div className="input-group">
              <label className="input-label">
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: "var(--teal-accent)" }}>lan</span>
                <span>Servidor del backend</span>
              </label>
              <input
                type="text"
                className="input-field mono-text"
                style={{ fontSize: "0.85rem" }}
                placeholder="https://… o http://…"
                {...register("backendUrl", { onChange: onBackendChange })}
              />
              {errors.backendUrl && (
                <span style={{ color: "#b91c1c", fontSize: "0.75rem", fontWeight: 600 }}>
                  {errors.backendUrl.message}
                </span>
              )}
            </div>

            {/* Campo 2: Nombre de Jugador */}
            <div className="input-group">
              <label className="input-label">
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: "var(--teal-accent)" }}>badge</span>
                <span>Nombre de jugador</span>
              </label>
              <input
                type="text"
                className="input-field"
                style={{ fontWeight: 700 }}
                placeholder="Ej. HackerPoderoso"
                {...register("playerName")}
              />
              {errors.playerName && (
                <span style={{ color: "#b91c1c", fontSize: "0.75rem", fontWeight: 600 }}>
                  {errors.playerName.message}
                </span>
              )}
            </div>

            {/* Cuadro informativo */}
            <div className="configuration-note">
              <span className="mono-text configuration-note-label">
                {connectionStatus === "verified"
                  ? "CONEXIÓN VERIFICADA · HTTP 200"
                  : connectionStatus === "checking"
                    ? "VERIFICANDO SERVIDOR"
                    : "SIN CONECTAR"}
              </span>
              <p>
                Ajusta los parámetros y verifica la conexión antes de avanzar a la sala.
              </p>
            </div>

          </div>

          {healthError && (
            <div className="error-banner" role="alert">
              {healthError}
            </div>
          )}

          {/* Botón principal */}
          <div style={{ paddingTop: "1rem", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "flex-end" }}>
            <button type="submit" disabled={isSubmitting || connectionStatus === "checking"} className="btn-teal">
              <span>{isSubmitting ? "Conectando..." : "Continuar a la Sala"}</span>
              <span className="material-symbols-outlined" style={{ fontSize: 20 }}>arrow_forward</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default Configuracion;