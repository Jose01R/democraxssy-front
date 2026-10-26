# Pendientes del frontend

Revisión hecha al integrar el frontend con la infraestructura (23 de octubre),
actualizada el 25 de octubre después de integrar las correcciones de Paolo
(PR #4) y de jugar una partida completa en producción con 4 pestañas. Ver
también `ESTADO_DEL_PROYECTO.md`.

---

## 1. Requisitos del enunciado

- [x] **Cambiar de servidor desde la interfaz:** botón "Cambiar servidor" en
  `Entrada`.
- [x] **Validar el ID de partida:** expresión regular (hexadecimal de 32) antes
  de llamar al backend.

## 2. Fallas encontradas en la partida de prueba (25 de octubre)

- [ ] **Los mensajes de error se borran a los 2 segundos** (`src/state/partida.ts`,
  `refrescar`). El polling hace `set({ partida: v, error: null })` y borra
  cualquier error de una acción. Al votar dos veces, el `PUT 409` llega (queda en
  el panel de llamadas) pero el mensaje nunca se ve. Corrección: que `refrescar`
  solo borre los errores que puso él mismo.
- [ ] **Los jugadores quedan atrapados al eliminar la partida.** Los demás ven
  "Partida no encontrada", pero el polling sigue recibiendo `404` cada 2 segundos
  y no tienen cómo salir. Corrección: ante un 404 en `refrescar`, llamar a
  `limpiarSesion()` y mostrar "La partida ya no existe".
- [ ] **`GET 401` justo después de unirse** (menor): el primer polling parece
  adelantarse al token; el siguiente ya da 200. No rompe nada, pero deja una fila
  roja en el panel.

## 3. Interfaz

### Hecho

- [x] Cartas con aspecto de cartas, pantalla de resultado, pantalla final y
  clasificación (PR #2).
- [x] Copiar ID, jugadores faltantes, quién respondió (✓) y quién votó (🗳️).
- [x] Un solo título; estado honesto ("Sin conectar" → "Verificando conexión" →
  "Mesa Verificada" según el health check).
- [x] `font: inherit` en los campos, panel de llamadas con estilo de tarjeta,
  `index.html` en español y `App.css` eliminado.
- [x] Mensajes 409 según la acción (iniciar, responder, votar, decidir, avanzar).
  Falta que se vean: ver la falla 1 de la sección 2.
- [x] Fuentes e íconos locales con npm (solo alfabeto latino; la fuente de íconos
  pesa 323 KB).

### Ideas opcionales

- [ ] **Mostrar si la conexión es directa o por proxy** junto al estado de la
  mesa (por ejemplo "Mesa Verificada · conexión directa" o "· vía proxy").
  Ayuda a demostrar el requisito de backends HTTP y HTTPS.
- [ ] **Referencia visible a la protección XSS**, por ejemplo una nota de que
  nombres y cartas se muestran siempre como texto.
- [ ] **Panel de llamadas cerrado por defecto**, con un botón "Mostrar llamadas
  (N)". Con el polling se llena de `GET` repetidos y llega al tope de 100 filas;
  agrupar o filtrar los `GET` de estado haría más fácil ver las llamadas
  importantes en la demo.
- [ ] **Contraste en la votación vista por el soberano:** las respuestas se ven
  muy tenues mientras espera.
- [ ] Pasar a clases los `style={{...}}` que quedan en `Configuracion`.

## 4. Pruebas

- [x] **Partida completa en producción con 4 pestañas** (25 de octubre): crear,
  unirse ×3, iniciar, responder, votar, decidir, pantalla final y eliminar, todo
  con respuestas 2xx. Consola sin errores.
- [x] **XSS:** un jugador llamado `<svg onload=alert(1)>` aparece como texto en
  todas las pantallas y no se crea ninguna etiqueta. Nota: `<script>alert(1)</script>`
  no sirve como nombre porque tiene 25 caracteres (el máximo es 24).
- [ ] **Repetir la partida con 2 puntos para ganar**, para probar la pantalla de
  resultado y la operación **avanzar** (con 1 punto la partida termina directo).
- [ ] Probar en producción después de cada despliegue, no solo en `npm run dev`.

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
