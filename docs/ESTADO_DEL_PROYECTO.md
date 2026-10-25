# Cards for democraXSSy — Estado del frontend

**Proyecto 1 · IF-6000 Redes en los Negocios · Grupo: Joel · Paolo · José Andrés**
**Entrega: 27 oct 16:00 · Presentación: 29 oct**

Este documento resume qué hay en el repositorio, qué requisitos del enunciado
ya se cumplen y qué falta. Pensado para que cualquiera del grupo se ubique y
para apoyar la defensa (cada decisión queda justificada).

---

## 1. Qué hay en el repositorio

Proyecto **React + TypeScript + Vite**, con ESLint. La base del frontend está
completa; el juego está cableado de punta a punta y las pantallas de juego son
un esqueleto funcional listo para pulir.

### Estructura

```
src/
  api/
    client.ts              Cliente Axios único (mutator de orval)
    callLog.ts             Registro en memoria de las llamadas
    generated/             Cliente + tipos generados por orval (NO editar a mano)
  schemas/
    config.ts              Validación Zod del formulario inicial
    generated/             Esquemas Zod de la API generados por orval
  state/
    partida.ts             Capa de datos: sesión, acciones, polling (store)
  components/
    CallPanel.tsx          Panel visible con cada llamada (método, URL, código, ms)
  screens/
    Configuracion.tsx      Formulario: servidor, nombre, puntos para ganar
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
  (nombre 1-24, puntos 1-20).
- **XSS:** nombres y cartas se muestran siempre como texto. React escapa solo;
  no se usa `dangerouslySetInnerHTML` en ningún lado.
- **Sesión por pestaña** (`sessionStorage`): cada pestaña es un jugador distinto,
  necesario para probar con el mínimo de 4 jugadores.
- **Cliente generado con orval** desde el Swagger: las 10 operaciones y sus
  esquemas Zod salen del spec, no se escriben a mano.

---

## 2. Operaciones de la API (todas deben usarse)

Las 10 están disponibles en el store (`src/state/partida.ts`).

| Operación | Dónde se dispara | Estado |
| :-- | :-- | :-- |
| headApiSalud | App (al elegir servidor) | Cableada |
| crearPartida | Entrada | Cableada |
| unirse | Entrada | Cableada |
| obtenerPartida | Polling del store | Cableada |
| iniciar | Ronda (fase sala) | Cableada |
| responder | Ronda (fase respuestas) | Cableada (pulir UX) |
| votar | Ronda (fase votacion) | Cableada (pulir UX) |
| decidir | Ronda (fase decision) | Cableada (pulir UX) |
| avanzar | Ronda (fase resultado) | Cableada (pulir UX) |
| eliminarPartida | Ronda (fase terminada) | Cableada |

Máquina de estados de la ronda:
`sala -> respuestas -> votacion -> decision -> resultado -> (otra ronda | terminada)`

---

## 3. Cumplimiento del enunciado

| Requisito | Estado | Nota |
| :-- | :-- | :-- |
| Ejecutar y finalizar una partida completa | 🟡 En progreso | Flujo cableado; falta pulir pantallas y probar de principio a fin |
| Cambiar de servidor en la GUI (HTTP o HTTPS) | 🟢 Listo | Botón para volver a la configuración en `Entrada`. HTTPS directo; HTTP por el proxy `/api/http/` de Nginx |
| Desplegado en Internet con HTTPS | 🟢 Listo | https://democraxssy-if6000.duckdns.org/ (OCI + Nginx + Let's Encrypt). Desplegado el 24 oct con `scripts/deploy.ps1` |
| Seleccionar backend + nombre + (contraseña, nombre de juego) | 🟢 Hecho | Se usa backend, nombre y puntos. Sin contraseña ni nombre de juego: lo confirmó el profesor en clase |
| Todas las operaciones usadas por el frontend | 🟢 Hecho | Las 10 en el store |
| Datos validados en el frontend | 🟢 Hecho | Zod en config y en los bodys de la API; el ID de partida se valida con regex (hexadecimal de 32) |
| Todas las llamadas validadas y demostradas | 🟢 Mecanismo listo | Panel de llamadas + interceptor + pestaña Network |

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

## 5. Lo que falta

> Lista detallada y priorizada: ver **`PENDIENTES_FRONTEND.md`**.

**Frontend**
- Pulir las pantallas de juego de la `Ronda` (selección de cartas, mostrar votos,
  resultado y pantalla final) — hoy son un esqueleto funcional.
- Manejo fino de errores por fase (mensajes del 409 "acción fuera de fase").
- Demostrar el XSS: probar con nombres y cartas que traigan `<script>`.
- Pruebas en varios navegadores y del mínimo de 4 jugadores en número par.
- **Verificar que nuestra solución tenga las mismas funcionalidades que las que
  el profesor mostró en vivo.** Contrastar el flujo y cada pantalla contra esa
  demo y anotar cualquier comportamiento que falte o difiera.

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
- ✅ Script de despliegue `deploy.ps1` (sube el `dist/` de `npm run build`).
- Pendiente: primer despliegue del build y prueba de una partida completa.

---

## 6. Tareas para mañana

> ✅ **Resuelto:** pantalla en blanco. `src/api/client.ts` había quedado en la
> versión vieja (sin `setAuthToken`); se reemplazó por la que exporta
> `setAuthToken`/`getAuthToken`. La app ya carga sin error.

1. Correr `npm run dev` y confirmar el flujo completo hasta la fase `sala`, con
   el health check apareciendo en el panel de llamadas.
2. (Paolo) Empezar a pulir la pantalla de respuestas.
3. (José Andrés) Conectar el polling con el manejo de errores 409 por fase.
4. ~~Enviar el correo al profesor~~ — resuelto en clase: el proxy no es
   obligatorio y se puede usar dominio.
5. Revisar si nuestra solución tiene las mismas funcionalidades que las que el
   profesor mostró en vivo, y anotar diferencias.

---
*Universidad de Costa Rica · ECCI · IF-6000*
