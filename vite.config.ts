// vite.config.ts
//
// En desarrollo, el navegador corre en http://localhost, así que el problema de
// contenido mixto no aparece y podrían llamar http:// directo. Pero para que el
// MISMO código funcione igual en local y en producción, aquí se imita el proxy
// de Nginx: /api/http/host:puerto/ruta -> http://host:puerto/ruta
// (el destino sale de la propia URL; no hay que configurar nada).
// Solo aplica a `npm run dev`; en producción lo hace Nginx.

import http from "node:http";
import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";

function proxyHttpDev(): Plugin {
  return {
    name: "proxy-http-dev",
    configureServer(server) {
      server.middlewares.use("/api/http", (req, res) => {
        const [, destino = "", ...resto] = (req.url ?? "").split("/");
        const [host, puerto] = destino.split(":");
        const salida = http.request(
          {
            host,
            port: puerto ? Number(puerto) : 80,
            path: "/" + resto.join("/"),
            method: req.method,
            headers: { ...req.headers, host },
          },
          (r) => {
            res.writeHead(r.statusCode ?? 502, r.headers);
            r.pipe(res);
          },
        );
        salida.on("error", () => {
          res.statusCode = 502;
          res.end("Bad Gateway");
        });
        req.pipe(salida);
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), proxyHttpDev()],
  build: {
    // Nunca incrustar archivos como data: URIs (Vite lo hace con los pequeños,
    // como subconjuntos de fuentes). La CSP del servidor no permite data: para
    // fuentes; así todo se sirve como archivo propio del sitio.
    assetsInlineLimit: 0,
  },
});
