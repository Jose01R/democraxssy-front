# Pruebas de las llamadas HTTP y HTTPS

Se corrió `tools/pruebas-contrato.mjs` contra el servidor del profesor y contra el nuestro, por cada vía de transporte. Se repite con:

```powershell
powershell -ExecutionPolicy Bypass -File tools\pruebas-transportes.ps1
```

Las salidas completas están en `07-pruebas/`, una por destino, junto con `resumen.md`.

## Resultado (24 nov 2026, antes de la fase C/D del backend)

| Destino | URL | Resultado |
|---|---|---|
| Profesor, HTTPS | `https://democraxssy.akamai.meseguercr.com` | 61/62 |
| Profesor, HTTP | `http://democraxssy.akamai.meseguercr.com` | 0/25 (responde 501) |
| Nuestro, HTTPS | `https://api-democraxssy-if6000.duckdns.org` | 5/25 |
| Nuestro, HTTP | `http://api-democraxssy-if6000.duckdns.org` | 5/25 |
| Nuestro, HTTP vía el proxy del frontend | `https://democraxssy-if6000.duckdns.org/api/http/api-democraxssy-if6000.duckdns.org` | 5/25 |

El script se detiene en "Crear partida" si esa operación falla. Por eso el servidor del profesor llega a 62 casos y el nuestro a 25.

## Qué se concluye

1. **Las tres vías hacia nuestro backend dan exactamente lo mismo.** Las salidas de HTTPS, HTTP y HTTP vía proxy coinciden caso por caso. Nginx (puertos 80 y 443) y el proxy `/api/http/` del frontend no alteran las respuestas. Que no agreguen cabeceras que disparen el 409 se confirma cuando el backend tenga la validación de la fase D.
2. **CORS funciona por las tres vías:** `HEAD /api/salud` → 200 y la verificación previa `OPTIONS` → 204 con `Access-Control-Allow-Origin: *`.
3. **Lo que falta es del backend, no de la infraestructura.** En la VM está el `.jar` de la fase A, que solo tiene `/api/salud`. Las 404 de "Crear partida" y las validaciones del 409 se resuelven con las fases C y D. Esta prueba se vuelve a correr con el backend completo (fase F).
4. **El servidor del profesor no atiende HTTP:** Akamai responde `501 Not Implemented` sin CORS. Por eso el botón "Nuestro backend (HTTP vía proxy)" solo tiene sentido con nuestro backend, que sí expone el puerto 80 sin redirección ni HSTS (decisión 6).
5. **El servidor del profesor acepta claves JSON duplicadas** (`{"nombre":"Ana","nombre":"Beto"}` → 201, se queda con el último valor). Es el único caso que falla; nuestro backend lo rechazará con 409 (decisión 12).
