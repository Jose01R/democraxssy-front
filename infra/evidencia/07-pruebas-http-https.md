# Pruebas de las llamadas HTTP y HTTPS

Se corre `tools/pruebas-contrato.mjs` contra el servidor del profesor y contra el nuestro, por cada vía de transporte:

```powershell
powershell -ExecutionPolicy Bypass -File tools\pruebas-transportes.ps1
```

Las salidas completas están en `07-pruebas/`, una por destino, junto con `resumen.md`. Cada corrida reemplaza la anterior.

## Resultado actual (25 nov 2026, backend con el juego completo)

| Destino | URL | Resultado |
|---|---|---|
| Profesor, HTTPS | `https://democraxssy.akamai.meseguercr.com` | 61/62 |
| Profesor, HTTP | `http://democraxssy.akamai.meseguercr.com` | 0/25 (responde 501) |
| Nuestro, HTTPS | `https://api-democraxssy-if6000.duckdns.org` | 29/40 |
| Nuestro, HTTP | `http://api-democraxssy-if6000.duckdns.org` | 29/40 |
| Nuestro, HTTP vía el proxy del frontend | `https://democraxssy-if6000.duckdns.org/api/http/api-democraxssy-if6000.duckdns.org` | 29/40 |

Además, `node tools/partida-completa.mjs <url>` jugó una partida completa de 4 jugadores **por las tres vías** hacia nuestro backend: crear, unirse, iniciar, responder, votar, decidir, `terminada`, eliminar → 204 y GET → 404, todo correcto.

Antes (24 nov, `.jar` de la fase A con solo `/api/salud`) nuestro backend daba 5/25.

## Qué se concluye

1. **Las tres vías hacia nuestro backend dan exactamente lo mismo, caso por caso.** Nginx (puertos 80 y 443) y el proxy `/api/http/` del frontend no alteran las respuestas.
2. **El juego funciona por las tres vías,** incluido el revelado por fase (respuestas anónimas en `votacion`, votos desde `decision` y autores al terminar).
3. **Los casos que faltan son de validación del backend,** la fase D pendiente:
   - `HEAD /api/salud` con `?x=1` o con `Authorization` → 200 (debe ser 409; lo resuelve el PR #27, aún sin integrar);
   - Content-Type `text/plain` → 415;
   - `X-HTTP-Method-Override` → 201;
   - campos desconocidos, faltantes o de tipo incorrecto en el JSON;
   - cuerpo en `GET` y en `iniciar`.

   El caso "Iniciar con campo en el cuerpo" inicia la partida de prueba, así que el script no llega a probar el resto del flujo (por eso 40 casos y no 62). En local, con el backend levantado directamente, el resultado es 32/40.
4. **Un caso lo responde Nginx, no el backend.** Con la cabecera `Authorization` repetida, Nginx (1.20.1) rechaza la solicitud con su propia página **400** en HTML, sin CORS, antes de que llegue a Spring: no admite esa cabecera repetida. Se comprobó con `curl` (`Server: nginx`). En local, sin Nginx, el backend responde 409. El servidor del profesor (Akamai/Envoy) responde 409.
5. **El servidor del profesor no atiende HTTP:** Akamai responde `501 Not Implemented` sin CORS. Por eso la demo por HTTP solo se hace con nuestro backend, que sí expone el puerto 80 sin redirección ni HSTS (decisión 6).
6. **El servidor del profesor acepta claves JSON duplicadas** (se queda con el último valor); es el único caso que falla. El nuestro las rechaza con 409 (decisión 12).
