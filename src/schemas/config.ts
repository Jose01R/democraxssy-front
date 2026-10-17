// src/schemas/config.ts
//
// Validación del formulario inicial: servidor, nombre y puntos para ganar.
// Límites alineados con la API: nombre 1-24, puntos 1-20 (default 5 en el backend).
//
// Escrito para Zod 4: z.strictObject, parámetro `error`, URL validada con el
// parser del navegador (sin .url(), deprecado).

import { z } from "zod";

export const configSchema = z.strictObject({
  backendUrl: z
    .string()
    .trim()
    .min(1, "Escribe la dirección del backend")
    .refine((u) => {
      try {
        const parsed = new URL(u);
        return parsed.protocol === "http:" || parsed.protocol === "https:";
      } catch {
        return false;
      }
    }, "La URL debe ser válida y empezar con http:// o https://"),
  playerName: z
    .string()
    .trim()
    .min(1, "Escribe tu nombre de jugador")
    .max(24, "Máximo 24 caracteres"),
  pointsToWin: z
    .number({ error: "Escribe un número" })
    .int("Debe ser un número entero")
    .min(1, "Mínimo 1 punto")
    .max(20, "Máximo 20 puntos"),
});

export type ConfigForm = z.infer<typeof configSchema>;
