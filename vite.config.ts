// vite.config.ts
//
// En desarrollo, el navegador corre en http://localhost, así que el problema de
// contenido mixto no aparece y podrían llamar http:// directo. Pero para que el
// MISMO código (que manda los http:// a /api/) funcione igual en local y en
// producción, aquí montamos un proxy /api -> backend http, imitando a Nginx.
//
// Cambiar 'target' por el backend HTTP que indique el profe.

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "http://CAMBIAR-POR-BACKEND-HTTP",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});
