// orval.config.ts  (en la raíz del proyecto, junto a package.json)
//
// Genera dos cosas desde el openapi.yaml que descargaste:
//   1. api/generated  -> una función por operación (crear partida, unirse, votar…)
//                        que pasa por nuestro customAxios (mutator).
//   2. schemas/generated -> un esquema Zod por modelo, para validar bodys/respuestas.
//
// Correr con:  npx orval     (o añadir "generate": "orval" a package.json)

import { defineConfig } from "orval";

export default defineConfig({
  api: {
    input: "./openapi.yaml",
    output: {
      mode: "single",
      target: "./src/api/generated/democraxssy.ts",
      client: "axios-functions",
      override: {
        // Todas las funciones generadas usan NUESTRO Axios (interceptor + 409 + proxy).
        mutator: {
          path: "./src/api/client.ts",
          name: "customAxios",
        },
      },
    },
  },
  zod: {
    input: "./openapi.yaml",
    output: {
      mode: "single",
      target: "./src/schemas/generated/democraxssy.zod.ts",
      client: "zod",
      // Para que Zod rechace campos de más (coherente con el 409 del backend)
      // orval expone una opción de "strict" en el cliente zod; revisar la forma
      // exacta en https://orval.dev/docs/guides/zod y activarla aquí en override.zod.
    },
  },
});
