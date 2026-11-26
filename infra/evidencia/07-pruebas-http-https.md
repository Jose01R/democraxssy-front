# Pruebas de las llamadas HTTP y HTTPS

Se corre `tools/pruebas-contrato.mjs` contra el servidor del profesor y contra el nuestro, por cada vía de transporte:

```powershell
powershell -ExecutionPolicy Bypass -File tools\pruebas-transportes.ps1
```

Las salidas completas están en `07-pruebas/`, una por destino, junto con `resumen.md`. Cada corrida reemplaza la anterior.

## Resultado final (25 nov 2026, `.jar` final de `main` en `90de7eb`)

| Destino | URL | Resultado |
|---|---|---|
| Profesor, HTTPS | `https://democraxssy.akamai.meseguercr.com` | 61/62 |
| Profesor, HTTP | `http://democraxssy.akamai.meseguercr.com` | 0/25 (responde 501) |
| Nuestro, HTTPS | `https://api-democraxssy-if6000.duckdns.org` | **61/62** |
| Nuestro, HTTP | `http://api-democraxssy-if6000.duckdns.org` | **61/62** |
| Nuestro, HTTP vía el proxy del frontend | `https://democraxssy-if6000.duckdns.org/api/http/api-democraxssy-if6000.duckdns.org` | **61/62** |

Además, `node tools/partida-completa.mjs <url>` jugó una partida completa de 4 jugadores **por las tres vías**: crear, unirse, iniciar, responder, votar, decidir, `terminada`, eliminar → 204 y GET → 404, todo correcto.

Contra el `.jar` levantado en local, sin Nginx, la prueba da **62/62**.

Evolución: 5/25 con el `.jar` de la fase A (24 nov) → 29/40 con el juego sin validación (25 nov) → 61/62 con el `.jar` final.

## Qué se concluye

1. **Las tres vías hacia nuestro backend dan exactamente lo mismo, caso por caso.** Nginx (puertos 80 y 443) y el proxy `/api/http/` del frontend no alteran las respuestas.
2. **El juego funciona completo por las tres vías,** incluido el revelado por fase (respuestas anónimas en `votacion`, votos desde `decision` y autores al terminar).
3. **El único caso que falla lo responde Nginx, no el backend.** Con la cabecera `Authorization` repetida, Nginx (1.20.1) rechaza la solicitud con su propia página **400** en HTML, sin CORS, antes de que llegue a Spring: no admite esa cabecera repetida. Se comprobó con `curl` (`Server: nginx`). Sin Nginx, el backend responde el 409 correcto (62/62 en local).
4. **El servidor del profesor no atiende HTTP:** Akamai responde `501 Not Implemented` sin CORS. Por eso la demo por HTTP solo se hace con nuestro backend, que sí expone el puerto 80 sin redirección ni HSTS (decisión 6).
5. **El único caso que falla en el servidor del profesor** es el de las claves JSON duplicadas: las acepta y se queda con el último valor. El nuestro las rechaza con 409 (decisión 12).
