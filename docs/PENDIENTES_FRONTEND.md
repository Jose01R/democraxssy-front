# Pendientes del frontend

Actualizado el 26 de octubre, después de integrar los PR #5, #6 y #7 y el último
ajuste de votación, y de jugar partidas completas de 4 jugadores en desarrollo
y en producción. Ver también `ESTADO_DEL_PROYECTO.md`.

---

## 1. Requisitos del enunciado

- [x] **Cambiar de servidor desde la interfaz:** botón "Cambiar servidor" en
  `Entrada`.
- [x] **Validar el ID de partida:** expresión regular (hexadecimal de 32) antes
  de llamar al backend.
- [x] **Partida completa** de principio a fin, probada en producción.

## 2. Fallas encontradas y corregidas

- [x] **Los mensajes de error se borraban a los 2 segundos** (PR #5). El polling
  solo borra los errores que puso él mismo, y también los borra cuando cambia la
  fase.
- [x] **Los jugadores quedaban atrapados al eliminar la partida** (PR #5). Ante un
  404 en el polling se limpia la sesión y se vuelve a la pantalla de acceso con
  el aviso "La partida ya no existe".
- [x] **Recargar la página la dejaba en blanco** (PR #7). La dirección del
  servidor no se guardaba; ahora se guarda por pestaña en `sessionStorage`,
  junto con el token.
- [x] **Con un token inválido, el polling reintentaba sin fin** (PR #7). Si la
  sesión guardada no se puede recuperar, se borra y se muestra un aviso. Un
  corte de red sí se sigue reintentando.
- [x] **No se podía votar por la propia respuesta**, aunque la API lo permite
  ("Se permite votar por la propia respuesta", operación `votar`). Corregido
  directamente en `main`.
- [ ] **`GET 401` justo después de unirse** (menor, visto el 25 de octubre): el
  primer polling parecía adelantarse al token. No se volvió a revisar.

## 3. Interfaz

### Hecho

- [x] **Mesa de juego:** carta negra al centro, montón de respuestas boca abajo
  (una carta por respuesta, con animación), estado de la ronda y decoración
  (cenicero, café, cerveza) en SVG en línea.
- [x] **La carta negra se completa** con las cartas elegidas, con las que se
  bajaron y, en el resultado, con las de la ganadora. Todo como texto.
- [x] **Cambiar de carta con un clic:** si la selección está llena, la carta
  nueva reemplaza a la elegida primero.
- [x] **Barra de jugadores** con inicial, puntos, soberano, anfitrión y quién ya
  respondió o votó; ronda y meta a la derecha.
- [x] **Sala, espera, resultado y fin** en paneles oscuros. El resultado muestra
  la ganadora como carta destacada; el fin, la última frase ganadora y la
  clasificación.
- [x] **Votación y decisión:** etiqueta "Tu respuesta", botones bloqueados
  después de votar y etiqueta "Favorita del pueblo" (incluye empates).
- [x] **Puntos para ganar** solo en la tarjeta de Anfitrión (paso 2).
- [x] **Mensajes de error propios** por acción y código HTTP (400, 401, 403, 404,
  409, 413, 500). El texto del servidor queda en la consola; el código, en el
  panel de llamadas.
- [x] **Panel de llamadas** con Ocultar / Mostrar (visible por defecto).
- [x] Fuentes e íconos locales con npm (solo alfabeto latino).

### Ideas opcionales (no hechas)

- [ ] **Mostrar si la conexión es directa o por proxy** junto al estado de la
  mesa.
- [ ] **Nota visible sobre la protección XSS.**
- [ ] **Agrupar o filtrar los `GET` del polling** en el panel de llamadas.
- [ ] Pasar a clases los `style={{...}}` que quedan en `Configuracion` y
  `Entrada`.

## 4. Pruebas (26 de octubre)

- [x] **Partida completa en producción** con 4 pestañas y 2 puntos para ganar
  (4 rondas): resultado, avanzar, fin y eliminar. Consola sin errores de CSP.
- [x] **Recargar a mitad de ronda** en producción: la partida vuelve.
- [x] **Errores:** responder dos veces y votar dos veces (409), unirse con un ID
  que no existe (404), unirse a una partida ya empezada (409), servidor caído y
  token inválido. Cada uno muestra su mensaje.
- [x] **Votar por la propia respuesta:** el servidor lo acepta; con un empate
  1-1-1 las tres respuestas quedan como favoritas.
- [x] **XSS:** el nombre `<svg onload=alert(1)>` se ve como texto en la barra,
  la carta ganadora y la clasificación.
- [x] **Celular:** vista revisada a 375 px (modo celular del navegador).
- [ ] **Backend por HTTP a través del proxy de Nginx:** no hay uno para probar;
  el del profesor es HTTPS.
- [ ] **403, 400, 413 y 500:** la interfaz evita los primeros dos y los últimos
  dependen del servidor; los mensajes están, pero no se vieron en pantalla.
- [ ] **Otros navegadores y un teléfono real.**

## Notas de integración con la infraestructura

- El sitio tiene una **Content-Security-Policy** estricta: no usar scripts
  externos ni inline, ni fuentes o íconos por CDN (instalarlos con npm), ni
  `dangerouslySetInnerHTML`. Si algo no carga, la consola del navegador dice
  qué bloqueó la CSP.
- `vite.config.ts` tiene `assetsInlineLimit: 0` para que Vite no incruste
  archivos como `data:` (la CSP no permite fuentes en `data:`). No quitarlo.
- Antes de desplegar, correr **`npm run build`** (no solo `npm run dev`): es el
  que revisa los tipos de TypeScript.
- Para desplegar: `npm run build` y después
  `powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1` (ver README).
- Los backends `http://` pasan por el proxy `/api/http/host:puerto/ruta`;
  los `https://` se llaman directo. Eso ya lo resuelve `client.ts`.
- No correr `npm audit fix --force`: baja la versión de orval y rompe la
  generación del cliente.

---
*Universidad de Costa Rica · ECCI · IF-6000*
