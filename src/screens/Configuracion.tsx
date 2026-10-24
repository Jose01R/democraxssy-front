import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { configSchema, type ConfigForm } from "../schemas/config";
import { setBackend } from "../api/client";

type Props = {
  onReady: (cfg: ConfigForm) => void;
};

export function Configuracion({ onReady }: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ConfigForm>({
    resolver: zodResolver(configSchema),
    defaultValues: {
      backendUrl: "https://democraxssy.akamai.meseguercr.com",
      playerName: "",
      pointsToWin: 10,
    },
  });

  const onSubmit = handleSubmit((data) => {
    setBackend(data.backendUrl);
    onReady(data);
  });

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

          <div className="mono-text" style={{
            padding: "0.3rem 0.75rem",
            backgroundColor: "#e8dbcc",
            color: "#8f6d05",
            fontSize: "0.75rem",
            fontWeight: 700,
            borderRadius: 4,
            display: "flex",
            alignItems: "center",
            gap: "0.3rem"
          }}>
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>verified</span>
            <span>Mesa Verificada</span>
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
                {...register("backendUrl")}
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

            {/* Campo 3: Puntos para ganar */}
            <div className="input-group">
              <label className="input-label">
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: "var(--teal-accent)" }}>workspace_premium</span>
                <span>Puntos para ganar (Meta)</span>
              </label>
              <input
                type="number"
                min={1}
                max={100}
                className="input-field mono-text"
                style={{ fontWeight: 700, fontSize: "1rem" }}
                {...register("pointsToWin", { valueAsNumber: true })}
              />
              {errors.pointsToWin && (
                <span style={{ color: "#b91c1c", fontSize: "0.75rem", fontWeight: 600 }}>
                  {errors.pointsToWin.message}
                </span>
              )}
            </div>

            {/* Cuadro informativo */}
            <div style={{
              backgroundColor: "rgba(236, 228, 218, 0.6)",
              borderRadius: 8,
              padding: "0.85rem",
              border: "1px solid var(--border-color)",
              display: "flex",
              flexDirection: "column",
              justifyContent: "center"
            }}>
              <span className="mono-text" style={{ fontSize: "0.7rem", fontWeight: 700, color: "#4d3d32", marginBottom: "0.25rem" }}>
                CONEXIÓN LISTA
              </span>
              <p style={{ margin: 0, fontSize: "0.8rem", color: "var(--text-muted)", lineHeight: 1.4 }}>
                Ajusta los parámetros para avanzar a la sala pública o crear tu mesa privada.
              </p>
            </div>

          </div>

          {/* Botón principal */}
          <div style={{ paddingTop: "1rem", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "flex-end" }}>
            <button type="submit" disabled={isSubmitting} className="btn-teal">
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