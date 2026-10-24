# Pendientes del frontend (revisión del 23 de octubre)

Revisión hecha al integrar el frontend con la infraestructura. El flujo del
juego funciona de punta a punta; lo que sigue es lo que falta, ordenado por
prioridad. Ver también `ESTADO_DEL_PROYECTO.md`.

---

## 1. Urgente: requisitos del enunciado

- [ ] **Cambiar de servidor desde la interfaz.** El enunciado dice: *"El jugador
  debe poder cambiar de servidor en la interfaz gráfica"*. Después de configurar,
  la pantalla de crear o unirse (`Entrada` en `App.tsx`) no tiene forma de volver
  a la configuración. Falta un botón **"Cambiar servidor"** que limpie la sesión
  y regrese al formulario inicial.
- [ ] **Validar el ID de partida.** El enunciado pide que **todos** los datos se
  validen en el frontend. El formulario inicial usa Zod, pero el campo "ID de
  partida para unirse" es un input libre. El ID es hexadecimal de 32 caracteres:
  validarlo con Zod antes de llamar al backend y mostrar el error en pantalla.

## 2. Interfaz

- [ ] **Que las cartas parezcan cartas:** la carta negra con el texto a completar
  y las de la mano como cartas blancas seleccionables, en vez de *checkboxes*.
- [ ] **Limpiar la plantilla de Vite:** `App.css` todavía tiene estilos del
  ejemplo que no se usan (`.hero`, `#next-steps`, `#docs`, `.ticks`...), y
  `index.css` deja el `#root` con borde y un `h1` de 56px. En `index.html`,
  cambiar `lang="en"` por `lang="es"` y el título `democraxssy-front` por
  `Cards for democraXSSy`.
- [ ] **Pasar los estilos a CSS:** hoy están escritos dentro de los componentes
  (`style={{...}}`). Pasarlos a clases hace más fácil darle un estilo uniforme.
- [ ] **Pantalla de resultado de la ronda:** mostrar quién ganó y destacar cuando
  el soberano eligió una respuesta distinta a la más votada (el chiste satírico
  del juego).
- [ ] **Pantalla final:** ganador de la partida y puntajes finales.
- [ ] **Detalles útiles:** botón para copiar el ID de la partida, cuántos jugadores
  faltan para poder iniciar, quién ya respondió (`ya_respondio`) o votó (`ya_voto`).

## 3. Para terminar de probar

- [ ] **Errores entendibles:** convertir los 409 en mensajes según la fase, por
  ejemplo *"todavía no es tu turno"* o *"la votación aún no está abierta"*.
- [ ] **Demostrar el XSS:** jugar con un nombre como `<script>alert(1)</script>`
  y mostrar que aparece como texto. Sirve como evidencia para la defensa.
- [ ] **Partida completa con 4 pestañas** (número par), de la sala hasta la
  pantalla final, comparada con la demo que mostró el profesor.
- [ ] **Probar en producción:** después de cada despliegue, repetir la prueba en
  https://democraxssy-if6000.duckdns.org/ (no solo en `npm run dev`).

## Notas de integración con la infraestructura

- El sitio tiene una **Content-Security-Policy** estricta: no usar scripts
  externos ni inline, ni fuentes o íconos por CDN (instalarlos con npm), ni
  `dangerouslySetInnerHTML`. Si algo no carga, la consola del navegador dice
  qué bloqueó la CSP.
- Antes de pedir un despliegue, correr **`npm run build`** (no solo `npm run dev`):
  es el que revisa los tipos de TypeScript.
- Los backends `http://` pasan por el proxy `/api/http/host:puerto/ruta`;
  los `https://` se llaman directo. Eso ya lo resuelve `client.ts`.

---
*Universidad de Costa Rica · ECCI · IF-6000*
