# Cards for democraXSSy — Estado del frontend

**Proyecto 1 · IF-6000 Redes en los Negocios · Grupo: Joel · Paolo · José Andrés**
**Entrega: 27 oct 16:00 · Presentación: 29 oct**

Este documento resume qué hay en el repositorio, qué requisitos del enunciado
ya se cumplen y qué falta. Pensado para que cualquiera del grupo se ubique y
para apoyar la defensa (cada decisión queda justificada).

---

## 1. Qué hay en el repositorio

Proyecto **React + TypeScript + Vite**, con ESLint. La interfaz y las pantallas
principales están implementadas; queda validar el flujo completo con varios
jugadores y contrastarlo con la demo del profesor.

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
    Entrada.tsx            Crear una partida o unirse con su ID
    Ronda.tsx              Pantalla de juego (cambia según la fase)
  App.tsx                  Router: config -> entrada -> juego
openapi.yaml               Spec del profesor (fuente para orval)
orval.config.ts            Config de generación (cliente axios + zod)
vite.config.ts            Proxy /api de desarrollo (para backends HTTP)
```

### Decisiones técnicas (para justificar en la defensa)

- **Un solo cliente Axios** por el que pasan todas las llamadas. Registra cada
  petición (alimenta el panel y la pestaña Network) y hace explícito el 409.
- **Regla del 409 respetada:** no se envían cabeceras de más. El token `Bearer`
  se inyecta **solo** en las rutas que lo admiten; `salud`, `crear` y `unirse`
  nunca lo llevan. La API tampoco admite parámetros de consulta.
- **Backends HTTP y HTTPS:** `https://` se llama directo (CORS abierto); `http://`
  pasa por `/api/` en Nginx (si no, el navegador lo bloquea por contenido mixto).
- **Validación con Zod 4** en el frontend; límites alineados con la API
  (nombre 1-24, puntos 1-20).
- **Health check:** antes de entrar a la sala, el endpoint debe responder HTTP
  200. La interfaz indica "Sin conectar" hasta completar esta verificación.
- **Errores 409:** se muestran mensajes orientados a la acción y fase del juego;
  se recuerda revisar también el contrato de la API.
- **XSS:** nombres y cartas se muestran siempre como texto. React escapa solo;
  no se usa `dangerouslySetInnerHTML` en ningún lado.
- **CSP:** fuentes e íconos se sirven desde npm local; no hay imports de Google
  Fonts ni recursos de fuentes externos.
- **Sesión por pestaña** (`sessionStorage`): cada pestaña es un jugador distinto,
  necesario para probar con el mínimo de 4 jugadores.
- **Cliente generado con orval** desde el Swagger: las 10 operaciones y sus
  esquemas Zod salen del spec, no se escriben a mano.

---

## 2. Operaciones de la API (todas deben usarse)

Las 10 están disponibles en el store (`src/state/partida.ts`).

| Operación | Dónde se dispara | Estado |
| :-- | :-- | :-- |
| headApiSalud | App (al continuar desde configuración) | Cableada; requiere HTTP 200 |
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
| Cambiar de servidor en la GUI (HTTP o HTTPS) | 🟢 Frontend listo | Falta cerrar a qué backend HTTP reenvía `/api/` (infra) |
| Desplegado en Internet con HTTPS | 🟢 Hecho | Primera versión en producción el 24 oct: https://democraxssy-if6000.duckdns.org/ |
| Seleccionar backend + nombre + (contraseña, nombre de juego) | 🟡 Parcial | Se usa backend, nombre y puntos. Sin contraseña ni nombre de juego por confirmación del profesor: **falta guardar ese correo como respaldo** |
| Todas las operaciones usadas por el frontend | 🟢 Hecho | Las 10 en el store |
| Datos validados en el frontend | 🟢 Hecho | Zod en config y en los bodys de la API |
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

**Frontend**
- Completar una partida con 4 pestañas (número par), desde la sala hasta la
  pantalla final, y revisar las pantallas de juego en celular.
- Demostrar el XSS: probar con un nombre como `<script>alert(1)</script>` y
  confirmar que se muestra como texto.
- Probar en producción después de cada despliegue.
- **Verificar que nuestra solución tenga las mismas funcionalidades que las que
  el profesor mostró en vivo.** Contrastar el flujo y cada pantalla contra esa
  demo y anotar cualquier comportamiento que falte o difiera.

**Infraestructura**
- Primera versión desplegada por HTTPS el 24 de octubre. El health check contra
  el backend responde 200; validar de nuevo después de cada despliegue.

**Gestión**
- Enviar al profesor el correo con la decisión del proxy y la confirmación de
  que no van contraseña ni nombre de juego. Guardarlo como respaldo.

---

## 6. Pendientes actuales

> ✅ **Resuelto:** pantalla en blanco. `src/api/client.ts` había quedado en la
> versión vieja (sin `setAuthToken`); se reemplazó por la que exporta
> `setAuthToken`/`getAuthToken`. La app ya carga sin error.

1. Completar una partida con cuatro pestañas y validar la experiencia móvil.
2. Confirmar visualmente en el navegador que un nombre `<script>alert(1)</script>`
   se representa como texto y no se ejecuta.
3. Comparar el flujo completo y las pantallas contra la demo del profesor.
4. Verificar el sitio de producción después del siguiente despliegue.

---
*Universidad de Costa Rica · ECCI · IF-6000*
