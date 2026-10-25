# Cards for democraXSSy — Frontend

Frontend del juego de cartas (temática XSS) para **IF-6000 · Redes en los Negocios**.
React + TypeScript + Vite. El cliente HTTP y los esquemas de validación se generan
desde el Swagger del backend con orval.

> Grupo: Joel · Paolo · José Andrés
> Estado del proyecto y pendientes: ver **`ESTADO_DEL_PROYECTO.md`**.

---

## Requisitos

- **Node.js 22 LTS o superior.** Vite 8 necesita una versión reciente; si
  `npm install` o `npm run dev` se queja de la versión de Node, actualizá.
- npm (viene con Node).
- Git.

Comprobar versiones:
```bash
node -v
npm -v
```

---

## Arranque rápido

```bash
# 1. Clonar
git clone <URL-del-repo>
cd democraxssy-front

# 2. Instalar dependencias
npm install
```

Durante la instalación, npm puede avisar que **esbuild** tiene un script de
instalación sin aprobar (medida de seguridad de npm). Si pasa, aprobalo una vez:
```bash
npm install-scripts approve esbuild
```
Ya hay un `allowScripts` en `package.json`, así que normalmente no hace falta.

```bash
# 3. Generar el cliente y los esquemas desde el Swagger
npx orval

# 4. Correr en desarrollo
npm run dev
```
Abrí la URL que imprime (por defecto `http://localhost:5173`).

---

## Scripts

| Comando | Qué hace |
| :-- | :-- |
| `npm run dev` | Servidor de desarrollo con recarga en caliente |
| `npm run build` | Chequeo de tipos + build de producción en `dist/` |
| `npm run preview` | Sirve el `dist/` ya construido |
| `npm run lint` | ESLint sobre el proyecto |
| `npx orval` | Regenera `src/api/generated` y `src/schemas/generated` desde `openapi.yaml` |

> Sugerencia: agregar `"generate": "orval"` a los scripts de `package.json`
> para correrlo como `npm run generate`.

---

## Cómo usar la app

1. En la pantalla de configuración, elegí el **servidor** del backend, tu
   **nombre** y los **puntos para ganar**. Al continuar se hace un *health check*
   que aparece en el panel de llamadas (abajo); solo se avanza si responde HTTP
   200.
2. **Crear partida nueva** (quedás como anfitrión) o **Unirse** con el ID de una
   partida existente.
3. Hacen falta **4 jugadores en número par** para iniciar. Para probar solo,
   abrí varias pestañas: cada una configura el mismo servidor y se une con el ID
   que muestra la sala. Cada pestaña es un jugador distinto (la sesión va por
   pestaña, en `sessionStorage`).

---

## Estructura

```
src/
  api/
    client.ts        Cliente Axios único: registro de llamadas, 409, token Bearer, proxy http
    callLog.ts       Store del registro de llamadas
    generated/       Generado por orval — NO editar a mano
  schemas/
    config.ts        Validación Zod del formulario inicial
    generated/       Esquemas Zod de la API — NO editar a mano
  state/
    partida.ts       Capa de datos: sesión, acciones de la API, polling
  components/
    CallPanel.tsx    Panel visible con cada llamada (método, URL, código, ms)
  screens/
    Configuracion.tsx
    Entrada.tsx      Crear una partida o unirse con su ID
    Ronda.tsx        Pantalla de juego (cambia según la fase)
  App.tsx            Router: config -> entrada -> juego
openapi.yaml         Spec del backend (fuente de orval)
orval.config.ts      Config de generación
vite.config.ts       Incluye el proxy /api de desarrollo
```

---

## Convenciones del equipo

- **No editar `src/api/generated` ni `src/schemas/generated`.** Se regeneran con
  `npx orval` desde `openapi.yaml`. Si cambia el Swagger, se vuelve a generar.
- **El store `src/state/partida.ts` es el contrato de datos.** Las pantallas
  leen con `usePartida()` y disparan acciones del store; no llaman al cliente
  generado directamente.
- **Regla del 409:** el backend rechaza cualquier campo, parámetro o cabecera de
  más, y no admite parámetros de consulta. No agregar cabeceras a mano; el token
  Bearer ya se inyecta solo donde corresponde.
- **XSS:** nombres y cartas se muestran siempre como texto con `{}` (React escapa
  solo). **Nunca** usar `dangerouslySetInnerHTML`.
- Las fuentes Plus Jakarta Sans, Outfit y Material Symbols se sirven desde
  paquetes npm locales; no se cargan fuentes ni íconos desde CDN.
- **Ramas:** trabajar en ramas por tarea (p. ej. `feat/pantallas-juego`) y hacer
  merge; no empujar directo a `main`.
- `node_modules/` y `dist/` no se suben (ya están en `.gitignore`).

Reparto actual: infraestructura/OCI (Joel) · cliente HTTP, store y panel
(José Andrés) · pantallas del juego (Paolo).

---

## Backends HTTP vs HTTPS

- Un backend `https://` se llama **directo** desde el navegador (CORS abierto).
- Un backend `http://` no se puede llamar desde una página HTTPS (contenido
  mixto), así que pasa por `/api/`, que en producción reenvía Nginx y en
  desarrollo reenvía Vite. Ajustar el destino en `vite.config.ts`
  (placeholder `CAMBIAR-POR-BACKEND-HTTP`) cuando se defina con infraestructura.

---
*Universidad de Costa Rica · ECCI · IF-6000*
