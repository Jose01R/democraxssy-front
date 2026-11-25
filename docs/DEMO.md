# Guion cronometrado — Demostración y defensa en vivo

## Proyecto

**Cards for democraXSSy** — Curso IF-6000 Redes en los Negocios  
Equipo: **José Andrés** (backend), **Joel** (infraestructura y DevOps) y **Paolo** (frontend, mazos y documentación).

Duración total: **15:00 minutos**.

## Resumen ejecutivo y distribución de tiempos

| Bloque | Responsable principal | Herramienta / vista | Duración |
|---|---|---|---:|
| 1. Diagrama de arquitectura | Joel | Diapositiva o terminal con el diagrama | 1.5 min |
| 2. IaC y despliegue en caliente (adelantado antes de jugar) | Joel | Terminal en `infra/`; consola OCI y SSH | 2.5 min |
| 3. Partida completa por HTTPS directo | Paolo conduce; José Andrés apoya | 4 pestañas del navegador; `Ronda.tsx` | 4.0 min |
| 4. Partida por HTTP vía proxy | Paolo | Ventana/pestañas preparadas para el preset HTTP | 1.5 min |
| 5. Interoperabilidad con el servidor del profesor | José Andrés | Ventana preparada para HTTPS del profesor | 1.5 min |
| 6. Inspección de red y `CallPanel` | José Andrés | DevTools, **Network** y panel en la interfaz | 1.5 min |
| 7. Errores HTTP `409` | José Andrés | UI y PowerShell con `curl.exe` | 1.5 min |
| 8. Preguntas y cierre | Equipo completo | Matriz de preguntas probables | 1.0 min |
| **Total** | | | **15.0 min** |

### Checklist técnico previo a la defensa

- [ ] Preparar tres ventanas o grupos de pestañas independientes:
  - **Grupo A:** cuatro pestañas para la partida HTTPS; todas en Configuración con el preset **Nuestro backend (HTTPS)**.
  - **Grupo B:** cuatro pestañas nuevas para la partida HTTP vía proxy; después del despliegue, dejarlas unidas a una sala de prueba para poder empezar el flujo de ronda sin demoras.
  - **Grupo C:** una o dos pestañas para el servidor del profesor, en Configuración con **Servidor del profesor**.
- [ ] Cada pestaña debe tener su propia sesión de jugador en `sessionStorage`. No reutilizar las pestañas de una partida activa para cambiar de servidor: `App.tsx` muestra `Ronda` mientras haya una partida y no ofrece **Cambiar servidor** desde esa pantalla.
- [ ] Preparar el `.jar` que se va a desplegar y verificar acceso SSH/OCI a la VM `democraxssy-back-e2`; la sesión debe estar lista antes de la defensa.
- [ ] Abrir DevTools en una pestaña de juego, activar **Network**, marcar **Disable cache** y limpiar los registros.
- [ ] Verificar antes de comenzar:

```powershell
curl.exe -I https://api-democraxssy-if6000.duckdns.org/api/salud
curl.exe -I http://api-democraxssy-if6000.duckdns.org/api/salud
```

`HEAD /api/salud` debe responder **200** por ambos esquemas.
- [ ] Confirmar con José Andrés, antes de la presentación, qué validaciones de las fases C y D ya están integradas y se pueden demostrar sin presentarlas como trabajo pendiente.

## Bloque 1 — Diagrama de arquitectura (1:30)

**Responsable: Joel.** Abrir la explicación a 0:00. Aclarar que el frontend es la página desde la que el usuario entra, pero las llamadas HTTPS directas del API no pasan por el frontend.

```mermaid
flowchart LR
  subgraph Navegador["Navegador - pestañas independientes"]
    P1["Jugadores"]
  end

  FE["Frontend OCI\nNginx + proxy /api/http/\nAnti-SSRF"]
  DNS["API DuckDNS\napi-democraxssy-if6000.duckdns.org\n157.151.151.205"]
  NGINX["Nginx backend OCI\n443 HTTPS / 80 HTTP\nSin HSTS\nLet's Encrypt"]
  JVM["Spring Boot Java 21\n127.0.0.1:8080\n-Xmx256m"]
  PROF["Servidor del profesor\nhttps://democraxssy.akamai.meseguercr.com"]

  P1 -->|"HTTPS directo / CORS"| DNS
  P1 -->|"HTTP vía proxy"| FE
  FE -->|"/api/http/..."| DNS
  DNS --> NGINX
  NGINX -->|"proxy_pass"| JVM
  P1 -->|"HTTPS directo"| PROF
```

### Guion hablado

- **0:00–0:25 — Joel:** “Cada pestaña representa una sesión de jugador independiente. La llamada directa HTTPS del navegador va al backend, no atraviesa el frontend; el navegador y el servidor coordinan mediante CORS.”
- **0:25–0:55 — Joel:** “Solo la URL HTTP se transforma en una ruta `/api/http/` del frontend. Este proxy anti-SSRF evita el error de Mixed Content y aplica las barreras de Nginx y `nftables`.”
- **0:55–1:20 — Joel:** “El backend vive en una VM OCI `VM.Standard.E2.1.Micro` Always Free. Nginx escucha en 443 y 80, sin redirección obligatoria ni HSTS, y reenvía a Spring Boot en `127.0.0.1:8080`. El servicio configura `-Xmx256m` y `MemoryMax=500M`.”
- **1:20–1:30 — Joel:** “El servidor del profesor es la otra ruta HTTPS directa; su servicio no atiende HTTP, por eso la prueba HTTP se hace contra nuestro backend.”

## Bloque 2 — IaC y despliegue en caliente (2:30)

**Responsable: Joel.** Este bloque se adelanta deliberadamente: desplegar reinicia `democraxssy-back` y descarta las partidas que estén en memoria. Hacerlo antes de crear la partida evita interrumpir la demostración y permite detectar problemas temprano.

### Cronograma del bloque

- **0:00–0:25:** desde la terminal de VS Code, ejecutar en `infra/`:

```powershell
cd infra
terraform plan
```

Mostrar el estado sincronizado esperado:

```text
No changes. Your infrastructure matches the configuration.
```

- **0:25–1:55:** desplegar el `.jar` ya preparado. Sustituir la ruta de ejemplo por la ruta real:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1 -Jar "C:\ruta\al\democraxssy-back.jar"
```

`infra/deploy.ps1` sube el `.jar`, conserva la versión previa, reinicia `democraxssy-back` y espera a que `HEAD /api/salud` responda 2xx. Si falla, muestra el log y restaura la versión anterior cuando exista.

- **1:55–2:15:** señalar la verificación externa que ejecuta el script para ambos esquemas:

```text
http://api-democraxssy-if6000.duckdns.org/api/salud
https://api-democraxssy-if6000.duckdns.org/api/salud
```

Debe mostrar los códigos HTTP devueltos; comprobar que ambos son 2xx antes de comenzar la partida.

- **2:15–2:30:** mostrar brevemente OCI: instancia `democraxssy-back-e2`, forma `VM.Standard.E2.1.Micro` Always Free, IP reservada `157.151.151.205` y panel de métricas de CPU/memoria. Explicar el tope de `MemoryMax=500M` y JVM `-Xmx256m`.

**Frase clave:** “Terraform demuestra que la infraestructura ya coincide con el código; el despliegue del artefacto es una acción aparte, con health check y rollback.”

**Control del tiempo:** tener lista la ruta del `.jar`, SSH funcionando y consola OCI abierta. El script de despliegue puede esperar hasta 120 segundos; si el entorno supera el margen previsto, no inventar un resultado ni omitir la verificación: el cronograma requiere ensayo previo.

## Bloque 3 — Partida completa por HTTPS directo (4:00)

**Responsable: Paolo.** Usar únicamente el Grupo A preparado con **Nuestro backend (HTTPS)**. URL del preset: `https://api-democraxssy-if6000.duckdns.org`.

Usar meta de **1 punto** para que la primera decisión finalice la partida y la pantalla `terminada` muestre al ganador. La meta 2 es una variante si se prioriza mostrar resultado de ronda y rotación del soberano; no intentar hacer ambas variantes dentro de estos cuatro minutos.

| Tiempo | Acción y explicación |
|---|---|
| 0:00–0:20 | En las cuatro pestañas, comprobar **Nuestro backend (HTTPS)**. Completar nombres diferentes y el health check `HEAD /api/salud`; debe mostrar **Mesa Verificada** / **Conexión verificada · HTTP 200**. |
| 0:20–0:45 | En la pestaña anfitriona, fijar meta 1 y pulsar **Crear partida nueva** (`POST /api/partidas`). Copiar el ID de la partida. |
| 0:45–1:15 | En las otras tres pestañas, usar **Unirse** con el ID (`POST /api/partidas/{id}/jugadores`). Verificar que ya hay cuatro jugadores en `sala`. |
| 1:15–1:35 | El anfitrión inicia (`POST /api/partidas/{id}/iniciar`). Señalar el requisito contractual de 4–22 jugadores y cantidad par. |
| 1:35–2:20 | Los tres jugadores que no son soberano envían respuesta (`PUT /api/partidas/{id}/respuestas`). Mostrar **“Conexión directa (HTTPS)”**. Incluir en una carta o nombre la cadena literal `<script>alert('demo')</script>` y señalar que aparece como texto plano, no como HTML. |
| 2:20–2:55 | Los jugadores que pueden votar emiten su voto (`PUT /api/partidas/{id}/votos`). Al completarse la votación, la fase pasa a `decision`. |
| 2:55–3:25 | El soberano elige la respuesta (`POST /api/partidas/{id}/decision`). Con meta 1 se alcanza el objetivo de puntos y la partida queda `terminada`. |
| 3:25–4:00 | Señalar la pantalla **Fin de la partida**, el ganador y la tabla de posiciones. Aclarar que con meta 2 se puede mostrar, en una ronda adicional, `favoritas_del_pueblo`, `respeto_al_pueblo` y **Siguiente ronda** (`POST /api/partidas/{id}/avanzar`) para observar la rotación del soberano. |

`Ronda.tsx` pinta los nombres y cartas con JSX; no usar `dangerouslySetInnerHTML`.

## Bloque 4 — Partida por HTTP vía proxy (1:30)

**Responsable: Paolo.** Cambiar al Grupo B nuevo, con las cuatro sesiones ya unidas a una sala de prueba creada después del despliegue. El preset debe ser **Nuestro backend (HTTP vía proxy)**. No cambiar el servidor en las pestañas de la partida activa.

URL seleccionada: `http://api-democraxssy-if6000.duckdns.org`.

- **0:00–0:20:** señalar el preset exacto y el indicador **“Conexión vía proxy (HTTP)”**.
- **0:20–0:50:** explicar que `frontend/src/api/client.ts` convierte el destino HTTP en una petición relativa `/api/http/api-democraxssy-if6000.duckdns.org/api/...`. Así la página HTTPS no hace una petición HTTP activa directa y no aparece el error de *Mixed Content*.
- **0:50–1:15:** ejecutar una respuesta o voto en la sesión HTTP y comprobar que la ronda opera con normalidad.
- **1:15–1:30:** aclarar que este backend atiende HTTP en el puerto 80 sin HSTS ni redirección automática. El servidor del profesor no sirve HTTP (Akamai responde 501); la demostración HTTP se limita intencionalmente a nuestro backend.

## Bloque 5 — Interoperabilidad con el servidor del profesor (1:30)

**Responsable: José Andrés.** Usar el Grupo C preparado con el preset **Servidor del profesor**.

URL: `https://democraxssy.akamai.meseguercr.com`.

- **0:00–0:20:** ejecutar `HEAD /api/salud` por HTTPS.
- **0:20–0:40:** verificar **“Conexión directa (HTTPS)”** y mostrar el destino del servidor en `CallPanel`.
- **0:40–1:15:** realizar el flujo básico preparado (por ejemplo, crear una partida y consultar su sala), con las rutas, métodos y cuerpos descritos por `openapi.yaml`.
- **1:15–1:30:** recalcar compatibilidad de contrato. No intentar HTTP contra este servidor: Akamai responde 501 a esa ruta de demostración.

## Bloque 6 — Inspección de red y `CallPanel` (1:30)

**Responsable: José Andrés.** En DevTools, mantener **Network** abierto y la caché deshabilitada. `CallPanel` está integrado en `frontend/src/App.tsx` y visible por defecto.

- En una llamada autenticada, señalar el formato `Authorization: Bearer <64 caracteres hexadecimales>`; **no proyectar ni copiar el valor real del token**.
- Mostrar `Content-Type: application/json` cuando la petición tiene cuerpo.
- En respuestas, buscar `Access-Control-Allow-Origin: *`.
- Comparar destinos:
  - HTTPS directo: `https://api-democraxssy-if6000.duckdns.org/api/...`
  - HTTP vía proxy, visto desde el navegador: `https://democraxssy-if6000.duckdns.org/api/http/api-democraxssy-if6000.duckdns.org/api/...`
- En la interfaz, señalar en `CallPanel` las columnas **Hora**, **Método**, **Código**, **ms** y **URL**; pulsar **Limpiar** y generar otra llamada.

## Bloque 7 — Errores HTTP `409` (1:30)

**Responsable: José Andrés.** Ejecutar solo casos que ya estén integrados y comprobados en el backend del día de la defensa; los rechazos definidos en `openapi.yaml` no deben presentarse como código implementado si aún no están listos.

### 7.1 Desde la UI: intentar unir un quinto jugador a una partida iniciada

- En una pestaña nueva del mismo backend, intentar unirse a una partida cuya fase ya no sea `sala`.
- La ruta es `POST /api/partidas/{id}/jugadores`; el contrato restringe el ingreso a `sala`, así que el resultado esperado bajo esa regla es `409`.
- Mostrar el mensaje de la interfaz y el código en `CallPanel`. Si José no ha integrado esta validación, explicar que está en el contrato y no anunciarlo como respuesta implementada.

### 7.2 Desde PowerShell: solicitud inválida sin problemas de escape JSON

PowerShell 5.1 puede alterar las comillas del JSON al pasarlo a `curl.exe`. Escribir el cuerpo a un archivo ASCII y usar `--data-binary`:

```powershell
$bodyFile = Join-Path $env:TEMP "partida-demo-409.json"
'{"nombre":"Prueba409","puntos_para_ganar":5}' | Set-Content -Path $bodyFile -Encoding Ascii
curl.exe -i -X POST -H "Content-Type: application/json" --data-binary "@$bodyFile" "https://api-democraxssy-if6000.duckdns.org/api/partidas?test=1"
```

El parámetro `?test=1` debe rechazarse con `409` según `openapi.yaml`. Mostrar el estado y el cuerpo con forma `{"error":"..."}`; no memorizar ni afirmar un texto exacto hasta que José confirme la implementación.

Una cabecera prohibida (`X-HTTP-Method-Override`) o una clave JSON duplicada son alternativas para ensayar, pero ejecutarlas únicamente si José confirma que la fase D ya las implementa.

## Bloque 8 — Infraestructura, preguntas probables y cierre (1:00)

La demostración corta de Terraform, despliegue y métricas OCI se adelantó al **Bloque 2** para no reiniciar el backend después de crear partidas. Usar el minuto final para una pregunta rápida y cerrar; la matriz queda como guía de estudio. Cualquiera puede responder cualquier pregunta.

| Pregunta difícil | Respuesta clave / cuidado de precisión | Líder |
|---|---|---|
| ¿Cómo avanza la máquina de estados? | Explicar las fases y las condiciones contractuales de `openapi.yaml`; describir únicamente transiciones que se hayan probado en la versión desplegada. | José Andrés |
| ¿Por qué una acción fuera de fase o un parámetro de consulta puede dar `409`? | El contrato asigna `409` a conflictos de fase y solicitudes con parámetros/cabeceras/campos inesperados. Distinguir contrato de implementación ya integrada. | José Andrés |
| ¿Cómo detectan claves JSON duplicadas? | El contrato las prohíbe. Confirmar con José que la detección estricta de Jackson de fase D ya está integrada antes de afirmar el mecanismo implementado. | José Andrés |
| ¿Por qué el backend limita el cuerpo a 4 KB y Nginx acepta `8k`? | La configuración de Nginx deja pasar hasta 8 KB para que el backend pueda emitir el `413` JSON con CORS. Confirmar que el límite de 4 KB ya está integrado en el backend de la demo. | José Andrés / Joel |
| ¿Cómo resuelven concurrencia y estado en memoria sin base de datos? | La decisión arquitectónica es mantener las partidas efímeras en memoria y sin BD. Explicar la estrategia de sincronización solo después de comprobar el código actual con José. | José Andrés |
| ¿Se limpian automáticamente partidas inactivas? | No prometer limpieza por inactividad: está descrita como planeada en la ADR. Indicar el estado real que José confirme para la versión desplegada. | José Andrés |
| ¿Cómo se evita SSRF en el proxy HTTP? | El diseño usa una lista de destinos prohibidos en Nginx y reglas `nftables` para limitar conexiones del usuario `nginx` a redes privadas. | Joel |
| ¿Por qué no hay HSTS en el backend? | Se requiere demostrar HTTP plano por el proxy; HSTS haría que el navegador actualizara HTTP a HTTPS. Este backend atiende 80 y 443 sin HSTS. | Joel |
| ¿Qué aporta OCI Always Free y cómo se limita la memoria? | VM `VM.Standard.E2.1.Micro`, IP reservada y ejecución acotada por `MemoryMax=500M` y `-Xmx256m`; mostrar métricas reales de la instancia. | Joel |
| ¿Cómo evitan XSS al renderizar cartas? | React renderiza los textos como texto, mediante JSX; la pantalla no usa `dangerouslySetInnerHTML`. La CSP del frontend limita además los recursos permitidos. | Paolo |
| ¿Cómo se demuestra compatibilidad con el servidor del profesor? | Se selecciona su preset HTTPS y se ejecutan rutas y cuerpos del contrato `openapi.yaml`; su endpoint HTTP no se usa porque responde 501. | Paolo |

### Cierre verbal

“Mostramos el contrato del juego, el frontend, el proxy seguro para HTTP, la interoperabilidad HTTPS y el despliegue reproducible en OCI. Distinguimos lo que ya está verificado en la versión desplegada de lo que el contrato define y aún se encuentra en integración.”
