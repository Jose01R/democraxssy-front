# Cards for democraXSSy — Estado del frontend

**Proyecto 1 · IF-6000 Redes en los Negocios · Grupo: Joel · Paolo · José Andrés**
**Entrega: 27 oct 16:08 · Presentación: 27 oct**

*Actualizado el 26 de octubre.*

Este documento resume qué hay en el repositorio, qué requisitos del enunciado
ya se cumplen y qué falta. Pensado para que cualquiera del grupo se ubique y
para apoyar la defensa (cada decisión queda justificada).

---

## 1. Qué hay en el repositorio

Proyecto **React + TypeScript + Vite**, con ESLint. Todas las pantallas están
implementadas y el flujo completo está probado en producción con 4 jugadores.

### Estructura

```
src/
  api/
    client.ts              Cliente Axios único (mutator de orval)
    callLog.ts             Registro en memoria de las llamadas
    generated/             Cliente + tipos generados por orval (NO editar a mano)
  schemas/
    config.ts              Validación Zod del formulario inicial y de los puntos
    generated/             Esquemas Zod de la API generados por orval
  state/
    partida.ts             Capa de datos: sesión, acciones, polling (store)
  components/
    CallPanel.tsx          Panel visible con cada llamada (método, URL, código, ms)
    DecoracionMesa.tsx     Cenicero, café, cerveza y montón de cartas (SVG/CSS)
  screens/
    Configuracion.tsx      Formulario: servidor y nombre
    Entrada.tsx            Crear una partida (con puntos para ganar) o unirse con su ID
    Ronda.tsx              Pantalla de juego (cambia según la fase)
  App.tsx                  Router: config -> entrada -> juego
openapi.yaml               Spec del profesor (fuente para orval)
orval.config.ts            Config de generación (cliente axios + zod)
vite.config.ts            Proxy /api/http de desarrollo (imita al de Nginx)
```

### Decisiones técnicas (para justificar en la defensa)

- **Un solo cliente Axios** por el que pasan todas las llamadas. Registra cada
  petición (alimenta el panel y la pestaña Network) y hace explícito el 409.
- **Regla del 409 respetada:** no se envían cabeceras de más. El token `Bearer`
  se inyecta **solo** en las rutas que lo admiten; `salud`, `crear` y `unirse`
  nunca lo llevan. La API tampoco admite parámetros de consulta.
- **Backends HTTP y HTTPS:** `https://` se llama directo (CORS abierto); `http://`
  pasa por el proxy `/api/http/host:puerto/ruta` de Nginx (si no, el navegador lo
  bloquea por contenido mixto). El profesor confirmó en clase que el proxy no es
  obligatorio en esta entrega; se deja implementado porque la segunda lo requiere.
- **Validación con Zod 4** en el frontend; límites alineados con la API
  (nombre 1-24, puntos 1-20). Además, cada partida que llega del servidor se
  valida con el esquema Zod que genera orval: si llega algo que no es una
  partida, se muestra un mensaje en vez de romper la pantalla.
- **Health check:** antes de entrar a la sala, el endpoint debe responder HTTP
  200. La interfaz indica "Sin conectar" hasta completar esta verificación.
- **Mensajes de error propios:** cada acción tiene su mensaje según el código
  HTTP (400, 401, 403, 404, 409, 413, 500). El texto del servidor queda en la
  consola y el código en el panel de llamadas.
- **XSS:** nombres y cartas se muestran siempre como texto. React escapa solo;
  no se usa `dangerouslySetInnerHTML` en ningún lado.
- **CSP:** fuentes e íconos se sirven desde npm local; no hay imports de Google
  Fonts ni recursos de fuentes externos. La decoración de la mesa es SVG en
  línea dentro de los componentes, sin imágenes externas.
- **Sesión por pestaña** (`sessionStorage`): cada pestaña es un jugador distinto,
  necesario para probar con el mínimo de 4 jugadores. Se guardan el ID de la
  partida, el del jugador, el token y la dirección del servidor, para que una
  recarga vuelva a la misma partida.
- **Reglas de la API respetadas en la interfaz:** el soberano no responde ni
  vota; cada jugador vota una vez y **puede votar por su propia respuesta**
  (lo dice la operación `votar`). Los votos no eligen la ganadora: decide el
  soberano, y los empates solo afectan "respetar al pueblo".
- **Cliente generado con orval** desde el Swagger: las 10 operaciones y sus
  esquemas Zod salen del spec, no se escriben a mano.

---

## 2. Operaciones de la API (todas deben usarse)

Las 10 están disponibles en el store (`src/state/partida.ts`).

| Operación | Dónde se dispara | Estado |
| :-- | :-- | :-- |
| headApiSalud | App (al continuar desde configuración) | Probada; requiere HTTP 200 |
| crearPartida | Entrada | Probada |
| unirse | Entrada | Probada |
| obtenerPartida | Polling del store | Probada |
| iniciar | Ronda (fase sala) | Probada |
| responder | Ronda (fase respuestas) | Probada |
| votar | Ronda (fase votacion) | Probada |
| decidir | Ronda (fase decision) | Probada |
| avanzar | Ronda (fase resultado) | Probada |
| eliminarPartida | Ronda (fase terminada) | Probada |

Máquina de estados de la ronda:
`sala -> respuestas -> votacion -> decision -> resultado -> (otra ronda | terminada)`

---

## 3. Cumplimiento del enunciado

| Requisito | Estado | Nota |
| :-- | :-- | :-- |
| Ejecutar y finalizar una partida completa | 🟢 Listo | Probada en producción con 4 jugadores y 2 puntos (26 oct) |
| Cambiar de servidor en la GUI (HTTP o HTTPS) | 🟢 Listo | Botón para volver a la configuración en `Entrada`. HTTPS directo; HTTP por el proxy `/api/http/` de Nginx |
| Desplegado en Internet con HTTPS | 🟢 Listo | https://democraxssy-if6000.duckdns.org/ (OCI + Nginx + Let's Encrypt). Desplegado con `scripts/deploy.ps1`; última versión el 26 oct |
| Seleccionar backend + nombre + (contraseña, nombre de juego) | 🟢 Hecho | Se usa backend y nombre; los puntos los elige el anfitrión al crear. Sin contraseña ni nombre de juego: lo confirmó el profesor en clase |
| Todas las operaciones usadas por el frontend | 🟢 Hecho | Las 10 en el store |
| Datos validados en el frontend | 🟢 Hecho | Zod en config y en los bodys de la API; el ID de partida se valida con regex (hexadecimal de 32) |
| Todas las llamadas validadas y demostradas | 🟢 Hecho | Panel de llamadas (con Ocultar/Mostrar) + interceptor + pestaña Network; respuestas validadas con Zod |

---

## 4. Cómo correrlo en local

```bash
npm install
npx orval          # regenera cliente y esquemas desde openapi.yaml
npm run dev        # http://localhost:5173
```

Para probar una partida: elegir servidor (aparece el health check en el panel) →
"Crear partida nueva" → se entra en fase `sala`. Abrir 3 pestañas más, cada una
configura el mismo servidor y usa "Unirse" con el ID que muestra la sala
(se necesitan 4 jugadores en número par para iniciar).

---

## 5. Estado por área

> Lista detallada y priorizada: ver **`PENDIENTES_FRONTEND.md`**.

**Frontend**
- Hecho. Detalle de lo corregido, lo probado y lo que no se pudo probar en
  **`PENDIENTES_FRONTEND.md`**.

**Infraestructura (Joel)** — lista
- ✅ VM `VM.Standard.E2.1.Micro` (Always Free) en OCI, Oracle Linux 9, IP pública
  reservada `129.80.16.61`. Se intentó A1.Flex en los tres AD, sin capacidad en
  la región.
- ✅ Dominio `democraxssy-if6000.duckdns.org` (el profesor confirmó que se puede
  usar dominio) y certificados de Let's Encrypt para el dominio y la IP, con
  renovación automática y recarga de Nginx.
- ✅ Nginx: redirección HTTP → HTTPS, cabeceras de seguridad (CSP, X-Frame-Options,
  nosniff, Referrer-Policy, Permissions-Policy) y rutas de React (`try_files`).
- ✅ Proxy `/api/http/` para backends HTTP, con dos capas anti-SSRF: **lista de
  destinos prohibidos** en Nginx (localhost, redes privadas, metadatos de OCI,
  el propio servidor) y regla de **nftables** que impide al usuario `nginx` abrir
  conexiones a redes privadas aunque un dominio resuelva a ellas. No se usa lista
  blanca porque el usuario puede elegir cualquier servidor. Además: límite de
  peticiones por IP y log propio (`/var/log/nginx/proxy.log`).
- ✅ SELinux en modo enforcing.
- ✅ Certificado de la IP con el perfil de corta duración de Let's Encrypt
  (unos 6 días); se renueva solo. Revisarlo antes de la presentación.
- ✅ HSTS (`max-age=86400`) en el dominio; no en la IP, porque los navegadores
  lo ignoran en direcciones IP.
- ✅ SSH solo con llave: `passwordauthentication no`, `pubkeyauthentication yes`.
- ✅ Script de despliegue `deploy.ps1` (sube el `dist/` de `npm run build`).
- ✅ Primera versión desplegada el 24 de octubre; la última, el 26. El health
  check contra el backend responde 200.

---

## 6. Pendientes actuales

1. Antes de la presentación: abrir el sitio por el dominio y por la IP, revisar
   que los certificados estén vigentes y jugar una partida rápida de 4 pestañas.
2. Opcional: probar con un backend HTTP a través del proxy, si hay uno
   disponible.

---
*Universidad de Costa Rica · ECCI · IF-6000*
