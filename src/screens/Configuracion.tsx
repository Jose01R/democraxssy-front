// src/screens/Configuracion.tsx
//
// Pantalla inicial: el usuario elige el backend, su nombre y los puntos para ganar.
// Valida con Zod (via zodResolver) antes de dejar continuar, y fija el backend
// en el cliente Axios con setBackend().

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
    setBackend(data.backendUrl); // a partir de aquí el cliente apunta a este servidor
    onReady(data);
  });

  const fieldStyle = { display: "block", marginBottom: "1rem" } as const;
  const errStyle = { color: "#b4232a", fontSize: 13, marginTop: 4 } as const;

  return (
    <form onSubmit={onSubmit} style={{ maxWidth: 420, display: "grid", gap: 4 }}>
      <label style={fieldStyle}>
        <span>Servidor del backend</span>
        <input
          type="text"
          placeholder="https://… o http://…"
          style={{ width: "100%" }}
          {...register("backendUrl")}
        />
        {errors.backendUrl && <div style={errStyle}>{errors.backendUrl.message}</div>}
      </label>

      <label style={fieldStyle}>
        <span>Nombre de jugador</span>
        <input type="text" style={{ width: "100%" }} {...register("playerName")} />
        {errors.playerName && <div style={errStyle}>{errors.playerName.message}</div>}
      </label>

      <label style={fieldStyle}>
        <span>Puntos para ganar</span>
        <input
          type="number"
          min={1}
          max={100}
          style={{ width: "100%" }}
          // valueAsNumber: sin esto RHF entrega un string y Zod marcaría error de tipo
          {...register("pointsToWin", { valueAsNumber: true })}
        />
        {errors.pointsToWin && <div style={errStyle}>{errors.pointsToWin.message}</div>}
      </label>

      <button type="submit" disabled={isSubmitting}>
        Continuar
      </button>
    </form>
  );
}

export default Configuracion;
