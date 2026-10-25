# Pendientes del frontend

Revisión hecha al integrar el frontend con la infraestructura (23 de octubre),
actualizada el 24 de octubre después del merge del PR #2 (pantallas de Paolo)
y del primer despliegue en producción. Ver también `ESTADO_DEL_PROYECTO.md`.

---

## 1. Requisitos del enunciado

- [x] **Cambiar de servidor desde la interfaz.** La pantalla `Entrada` tiene un
  botón para volver a la configuración inicial (`onBack`), donde se puede elegir
  otro servidor.
- [x] **Validar el ID de partida.** `Entrada` lo valida con una expresión regular
  (hexadecimal de 32 caracteres) antes de llamar al backend. Opcional: pasarlo a
  Zod por consistencia con el resto de formularios.

## 2. Interfaz

### Hecho (PR #2)

- [x] **Cartas con aspecto de cartas:** carta negra y cartas blancas seleccionables.
- [x] **Pantalla de resultado de la ronda:** muestra la ganadora y si coincide o no
  con la más votada por la mesa.
- [x] **Pantalla final:** ganador y clasificación final.
- [x] **Detalles útiles:** botón para copiar el ID, cuántos jugadores faltan para
  iniciar, quién ya respondió (✓) y quién ya votó (🗳️).
- [x] **Fuentes e íconos locales:** Plus Jakarta Sans, Outfit y Material Symbols
  se instalan con npm y se importan en `main.tsx` (antes se cargaban desde Google
  Fonts y la CSP los bloqueaba en producción). No volver a usar `@import` de
  Google Fonts.
- [x] **Botón "Limpiar" del panel de llamadas** con el estilo `btn-secundario`.

### Pendiente

- [ ] **Título repetido:** "Cards for democraXSSy" aparece como encabezado de la
  página y otra vez dentro de la tarjeta de configuración. En celular ocupan casi
  media pantalla antes del primer campo. Dejar uno solo.
- [ ] **Indicadores de estado engañosos:** "Mesa Verificada" y "Conexión lista"
  aparecen antes de conectarse, pero el health check recién se hace al presionar
  "Continuar". En un proyecto de seguridad, un "verificado" que no verificó nada
  se puede cuestionar en la defensa. Mostrar "Sin conectar" y cambiar a
  verificado solo cuando el health check responda 200.
- [ ] **Fuente de los campos:** los `input` no heredan la fuente de la página
  (el texto "Ej. HackerPoderoso" se ve en otra letra). Agregar en `index.css`:
  `input, button, select, textarea { font: inherit; }`.
- [ ] **Panel de llamadas con el mismo estilo:** hoy es un recuadro oscuro con
  borde fino, distinto de las tarjetas color crema. Es lo que se muestra en la
  defensa para demostrar las llamadas, así que conviene que se vea integrado.
- [ ] **`index.html`:** cambiar `lang="en"` por `lang="es"` y el título
  `democraxssy-front` por `Cards for democraXSSy` (es lo que se ve en la pestaña
  del navegador).
- [ ] **Borrar `src/App.css`:** ya no se importa en ningún lado y solo tiene
  estilos de la plantilla de Vite.
- [ ] **Estilos dentro de los componentes:** quedan unos 90 `style={{...}}`
  (sobre todo en `Configuracion`, `Entrada` y `CallPanel`). Pasarlos a clases de
  `index.css` es opcional, pero hace más fácil mantener un estilo uniforme.

## 3. Para terminar de probar

- [ ] **Errores entendibles:** hoy se muestra el mensaje que manda el backend tal
  cual. Convertir los 409 en mensajes según la fase, por ejemplo *"todavía no es
  tu turno"* o *"la votación aún no está abierta"*.
- [ ] **Demostrar el XSS:** jugar con un nombre como `<script>alert(1)</script>`
  y mostrar que aparece como texto. Sirve como evidencia para la defensa.
- [ ] **Partida completa con 4 pestañas** (número par), de la sala hasta la
  pantalla final, comparada con la demo que mostró el profesor. Revisar también
  en esa partida las pantallas de juego en celular.
- [x] **Primer despliegue en producción:** el 24 de octubre en
  https://democraxssy-if6000.duckdns.org/. Carga sin errores de CSP y el health
  check contra el backend del profesor responde 200.
- [ ] **Probar en producción después de cada despliegue**, no solo en `npm run dev`.

## Notas de integración con la infraestructura

- El sitio tiene una **Content-Security-Policy** estricta: no usar scripts
  externos ni inline, ni fuentes o íconos por CDN (instalarlos con npm), ni
  `dangerouslySetInnerHTML`. Si algo no carga, la consola del navegador dice
  qué bloqueó la CSP.
- `vite.config.ts` tiene `assetsInlineLimit: 0` para que Vite no incruste
  archivos como `data:` (la CSP no permite fuentes en `data:`). No quitarlo.
- Antes de pedir un despliegue, correr **`npm run build`** (no solo `npm run dev`):
  es el que revisa los tipos de TypeScript.
- Para desplegar: `npm run build` y después
  `powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1` (ver README).
- Los backends `http://` pasan por el proxy `/api/http/host:puerto/ruta`;
  los `https://` se llaman directo. Eso ya lo resuelve `client.ts`.

---
*Universidad de Costa Rica · ECCI · IF-6000*
