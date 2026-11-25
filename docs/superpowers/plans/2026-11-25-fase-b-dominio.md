# Fase B — Dominio y máquina de estados · Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el núcleo del juego democraXSSy en memoria (modelo, máquina de estados, puntuación, mazos), testeable por completo sin HTTP.

**Architecture:** Por capas. `dominio/` son entidades (datos); `servicio/JuegoServicio` tiene toda la lógica (motor de estados, reglas, puntuación, mazo, revelado), es Java plano y se instancia directo en los tests sin contexto de Spring; `repositorio/` guarda las partidas en memoria. Las acciones inválidas lanzan excepciones tipadas (`errores/`) que la Fase D mapeará a códigos HTTP.

## Estructura de paquetes y convención

```
org.democraxssy
├── dominio/       entidades: Partida, Jugador, Ronda, Carta*, Respuesta, ResumenRonda, Fase, Mazo, Mazos
├── dto/           DTOs del contrato (neutral): Sesion, Vista, JugadorPublico, Yo, Ronda, RespuestaRevelada, ResumenRonda
├── servicio/      JuegoServicio (interfaz) + JuegoEnMemoria (impl) + Ids + CargadorMazo
├── repositorio/   RepositorioPartidas (interface) + RepositorioEnMemoria
└── errores/       excepciones de dominio
```

**Convención (acuerdo con Joel):** las entidades de `dominio/` son datos. **Todas las operaciones son métodos de la interfaz `servicio/JuegoServicio`** (impl `JuegoEnMemoria`), que **devuelven los DTOs de `dto/`** (`Sesion`/`Vista`) ya filtrados por jugador. Firmas acordadas:
```java
Sesion crearPartida(String nombre, Integer puntosParaGanar);
Sesion unirse(String partidaId, String nombre);
Vista  obtenerPartida(String partidaId, Optional<String> token);
void   eliminarPartida(String partidaId, String token);
Vista  iniciar(String partidaId, String token);
Vista  responder(String partidaId, String token, List<Integer> cartas);
Vista  votar(String partidaId, String token, String respuestaId);
Vista  decidir(String partidaId, String token, String respuestaId);
Vista  avanzar(String partidaId, String token);
```
Donde una tarea diga `Partida.crear(...)` / `p.unirse(...)`, léase `juego.crearPartida(...)` / `juego.unirse(id, ...)`.

**Tech Stack:** Java 21, JUnit 5 (de `spring-boot-starter-webmvc-test`, ya presente), Jackson (del starter) solo para cargar `cartas.json`. Sin dependencias nuevas.

**Spec:** `docs/superpowers/specs/2026-11-25-fase-b-dominio-design.md`

## Global Constraints

- Java 21. El paquete `org.democraxssy.dominio` **no importa nada de Spring**.
- Sin dependencias nuevas (Jackson y JUnit vienen del starter).
- `puntos_para_ganar`: entero 1–20, default **5** cuando es `null`.
- `Nombre`: trim, 1–24 caracteres, sin caracteres de control; único **sin distinguir mayúsculas**.
- Jugadores para iniciar: 4–22, número **par** (incluye al soberano).
- Mano objetivo: **7** cartas. `carta_negra.elegir` ∈ {1,2}.
- La partida termina **solo por puntos**; los mazos **reciclan descartes**, nunca se agotan.
- Ids: `partida` 32 hex, `jugador` 16 hex, `token` 64 hex, `respuesta` 12 hex, con `SecureRandom`.
- Barajado con semilla inyectable (`java.util.Random`) para tests deterministas.
- **Concurrencia:** los métodos mutadores de `Partida` (`unirse`, `iniciar`, `responder`, `votar`, `avanzar`, `decidir`) se declaran `synchronized` → lock por partida. No se añade test de concurrencia (los tests de dominio son monohilo).

## Review Focus

- **Rotación del soberano con vuelta:** tras el último en orden de llegada, el soberano vuelve al primero. → Task 8.
- **Carta negra con nº de `____` ≠ `elegir`:** el cargador la rechaza (falla rápido). → Task 3.
- **`respeto_al_pueblo` sin votos:** `true` cuando nadie votó; `false` cuando la ganadora no fue la más votada. → Task 9.
- **Mazo blanco agotado a mitad de reparto:** rebaraja descartes y completa; nunca lanza por falta de cartas. → Task 2.
- **Nombre duplicado por mayúsculas/espacios:** "ana" y " Ana " cuentan como el mismo. → Task 4.

---

### Task 1: Generador de identificadores

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/Ids.java`
- Test: `backend/src/test/java/org/democraxssy/dominio/IdsTest.java`

**Interfaces:**
- Produces: `Ids.partida()`, `Ids.jugador()`, `Ids.token()`, `Ids.respuesta()` → `String` (hex en minúscula de 32/16/64/12).

- [ ] **Step 1: Write the failing test**

```java
@Test void generaIdsConElLargoYFormatoCorrectos() {
  assertTrue(Ids.partida().matches("^[0-9a-f]{32}$"));
  assertTrue(Ids.jugador().matches("^[0-9a-f]{16}$"));
  assertTrue(Ids.token().matches("^[0-9a-f]{64}$"));
  assertTrue(Ids.respuesta().matches("^[0-9a-f]{12}$"));
  assertNotEquals(Ids.token(), Ids.token()); // aleatorio
}
```

- [ ] **Step 2: Run test to verify it fails** — `mvn -q test -Dtest=IdsTest`. Expected: FAIL (no existe `Ids`).
- [ ] **Step 3: Implement `Ids`** con `SecureRandom` y bytes → hex. Métodos estáticos de arriba.
- [ ] **Step 4: Run test** — Expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(dominio): generador de ids hex"`

---

### Task 2: Cartas y mazo con reciclaje

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/CartaBlanca.java` (record `id:int`, `texto:String`)
- Create: `backend/src/main/java/org/democraxssy/dominio/CartaNegra.java` (record `texto:String`, `elegir:int`)
- Create: `backend/src/main/java/org/democraxssy/dominio/Mazo.java` (genérico `Mazo<T>`: robo + descartes)
- Test: `backend/src/test/java/org/democraxssy/dominio/MazoTest.java`

**Interfaces:**
- Produces: `new Mazo<>(List<T> cartas, Random rng)`; `T robar()`; `void descartar(T)`; `void descartar(Collection<T>)`; `int disponibles()`. Al robar con robo vacío, baraja los descartes y los vuelve robo.

- [ ] **Step 1: Write the failing test** — tres casos:

```java
@Test void reparteHastaVaciarYLuegoReciclaDescartes() {
  Mazo<Integer> m = new Mazo<>(List.of(1,2,3), new Random(1));
  Integer a = m.robar(); Integer b = m.robar(); Integer c = m.robar();
  m.descartar(List.of(a,b,c));
  assertNotNull(m.robar()); // reciclo: no lanza aunque el robo estaba vacío
}
@Test void nuncaDevuelveLaMismaCartaDosVecesSinReciclar() {
  Mazo<Integer> m = new Mazo<>(List.of(1,2,3), new Random(1));
  Set<Integer> vistas = new HashSet<>(List.of(m.robar(), m.robar(), m.robar()));
  assertEquals(3, vistas.size());
}
```

- [ ] **Step 2: Run test** — Expected: FAIL.
- [ ] **Step 3: Implement** `CartaBlanca`, `CartaNegra`, `Mazo<T>`. `robar()`: si robo vacío y hay descartes, `Collections.shuffle(descartes, rng)` → robo.
- [ ] **Step 4: Run test** — Expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(dominio): cartas y mazo con reciclaje de descartes"`

---

### Task 3: Cargador y validador de mazo

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/CargadorMazo.java`
- Test: `backend/src/test/java/org/democraxssy/dominio/CargadorMazoTest.java`

**Interfaces:**
- Consumes: `CartaBlanca`, `CartaNegra` (Task 2), `MazoInvalidoException` (nueva aquí, runtime).
- Produces: `CargadorMazo.desdeJson(String json) → Mazos` donde `record Mazos(List<CartaNegra> negras, List<CartaBlanca> blancas)`. Valida: blancas con `id`≥0 y `texto` no vacío; negras con `elegir`∈{1,2} y **nº de `____` == `elegir`**. Si algo falla, `MazoInvalidoException`.

- [ ] **Step 1: Write the failing test**

```java
@Test void cargaUnMazoValido() {
  var m = CargadorMazo.desdeJson("""
    {"negras":[{"id":0,"texto":"a ____","elegir":1}],
     "blancas":[{"id":0,"texto":"x"}]}""");
  assertEquals(1, m.negras().size());
}
@Test void rechazaNegraConHuecosDistintosDeElegir() {
  assertThrows(MazoInvalidoException.class, () -> CargadorMazo.desdeJson(
    "{\"negras\":[{\"id\":0,\"texto\":\"a ____\",\"elegir\":2}],\"blancas\":[{\"id\":0,\"texto\":\"x\"}]}"));
}
```

- [ ] **Step 2: Run test** — Expected: FAIL.
- [ ] **Step 3: Implement** con `ObjectMapper`. Contar `____` con `split`/regex. Lanzar `MazoInvalidoException` con mensaje claro.
- [ ] **Step 4: Run test** — Expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(dominio): cargador y validador de cartas.json"`

---

### Task 4: Dominio base y fase `sala` (crear/unirse)

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/Fase.java` (enum: SALA, RESPUESTAS, VOTACION, DECISION, RESULTADO, TERMINADA)
- Create: `backend/src/main/java/org/democraxssy/dominio/Jugador.java` (id, nombre, token, puntos, mano:List<CartaBlanca>)
- Create: `backend/src/main/java/org/democraxssy/dominio/Partida.java`
- Create: exceptions `ConflictoException`, `ProhibidoException`, `DatosInvalidosException`, `NoEncontradoException` (todas `RuntimeException`) en `org.democraxssy.dominio.errores`
- Test: `backend/src/test/java/org/democraxssy/dominio/SalaTest.java`

**Interfaces:**
- Consumes: `Ids`, `Mazos`.
- Produces:
  - `Partida.crear(String nombreAnfitrion, Integer puntosParaGanar, Mazos mazos, Random rng) → Partida`. `null` en puntos → 5. Crea al anfitrión como primer jugador; fase SALA.
  - `Partida.unirse(String nombre) → Jugador` (token incluido). Solo en SALA; nombre válido y único (case-insensitive, trim); máx 22.
  - getters: `fase()`, `anfitrionId()`, `puntosParaGanar()`, `jugadores()` (orden de llegada), `jugadorPorToken(String)`.
  - Validación de nombre y excepciones reutilizadas por tasks siguientes.

- [ ] **Step 1: Write the failing test**

```java
@Test void crearDejaAlAnfitrionEnSalaConDefault5() {
  Partida p = Partida.crear("Ana", null, mazoDe(10,10), new Random(1));
  assertEquals(Fase.SALA, p.fase());
  assertEquals(5, p.puntosParaGanar());
  assertEquals(1, p.jugadores().size());
}
@Test void rechazaNombreDuplicadoPorMayusculasYEspacios() {
  Partida p = Partida.crear("Ana", 5, mazoDe(10,10), new Random(1));
  assertThrows(ConflictoException.class, () -> p.unirse("  ana "));
}
@Test void rechazaUnirseConMasDe22() { /* llenar a 22 y esperar ConflictoException */ }
```

- [ ] **Step 2: Run test** — Expected: FAIL.
- [ ] **Step 3: Implement** `Fase`, `Jugador`, excepciones y `Partida` (crear/unirse + validación de nombre). `mazoDe(n,b)` es un helper de test.
- [ ] **Step 4: Run test** — Expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(dominio): fase sala, crear partida y unirse"`

---

### Task 5: Registro de partidas en memoria

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/RepositorioPartidas.java` (interface)
- Create: `backend/src/main/java/org/democraxssy/dominio/RepositorioEnMemoria.java`
- Test: `backend/src/test/java/org/democraxssy/dominio/RepositorioEnMemoriaTest.java`

**Interfaces:**
- Produces: `void guardar(Partida)`; `Partida obtener(String id)` (lanza `NoEncontradoException` si no existe); `void eliminar(String id)`. Implementación con `ConcurrentHashMap`.

- [ ] **Step 1: Write the failing test** — guardar+obtener devuelve la misma; `obtener` de id inexistente lanza `NoEncontradoException`.
- [ ] **Step 2: Run test** — Expected: FAIL.
- [ ] **Step 3: Implement** interface + `ConcurrentHashMap`.
- [ ] **Step 4: Run test** — Expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(dominio): repositorio de partidas en memoria"`

---

### Task 6: Iniciar partida (reparto + primera ronda)

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/Ronda.java` (numero, soberanoId, cartaNegra, respuestas, favoritas, ganadora, votos/mapas internos)
- Modify: `Partida.java` (añadir `iniciar`, `ronda()`, estado de ronda)
- Test: `backend/src/test/java/org/democraxssy/dominio/IniciarTest.java`

**Interfaces:**
- Consumes: `Mazo`, `Ronda`, excepciones.
- Produces: `void iniciar(String tokenAnfitrion)`. Solo anfitrión; 4–22 jugadores y **par**; reparte 7 a cada uno; soberano = anfitrión; `Ronda` 1 con carta negra robada; fase → RESPUESTAS. `ronda()` devuelve la ronda actual (o `null` en SALA).

- [ ] **Step 1: Write the failing test**

```java
@Test void iniciarReparte7YPoneSoberanoAlAnfitrion() { /* 4 jugadores pares */
  // tras iniciar: fase RESPUESTAS, cada mano==7, soberano==anfitrion, ronda.numero==1 }
@Test void rechazaNumeroImparDeJugadores() { /* 5 jugadores -> ConflictoException */ }
@Test void rechazaSiNoEsAnfitrion() { /* token de otro -> ProhibidoException */ }
```

- [ ] **Step 2–5:** verificar fallo, implementar `Ronda` + `Partida.iniciar`, verificar PASS, commit `feat(dominio): iniciar partida y primera ronda`.

---

### Task 7: Responder (y auto-transición a `votacion`)

**Files:**
- Modify: `Partida.java` (`responder`), `Ronda.java` (registrar respuestas)
- Create: `backend/src/main/java/org/democraxssy/dominio/Respuesta.java` (id, autorId, cartas:List<CartaBlanca>)
- Test: `backend/src/test/java/org/democraxssy/dominio/ResponderTest.java`

**Interfaces:**
- Produces: `void responder(String token, List<Integer> cartaIds)`. Fase RESPUESTAS; el soberano no responde (`ProhibidoException`); exactamente `carta_negra.elegir` ids, sin repetir, todos en la mano (si no, `ConflictoException`/`DatosInvalidosException`); el orden se conserva; quita las cartas de la mano; crea `Respuesta` con `respuesta_id`. Cuando **todos los no-soberanos** respondieron: baraja las respuestas una vez (orden estable) y pasa a VOTACION.

- [ ] **Step 1: Write the failing test** — respuesta válida baja la mano y marca `ya_respondio`; soberano → `ProhibidoException`; cantidad ≠ `elegir` → rechazo; responder dos veces → `ConflictoException`; al responder el último no-soberano, fase → VOTACION.
- [ ] **Step 2–5:** fallo, implementar, PASS, commit `feat(dominio): responder y auto-transicion a votacion`.

---

### Task 8: Votar, avanzar y rotación del soberano

**Files:**
- Modify: `Partida.java` (`votar`, `avanzar`), `Ronda.java` (votos, favoritas)
- Test: `backend/src/test/java/org/democraxssy/dominio/VotarAvanzarTest.java`

**Interfaces:**
- Produces:
  - `void votar(String token, String respuestaId)`. Fase VOTACION; soberano no vota (`ProhibidoException`); un voto por jugador (repetir → `ConflictoException`); se permite la propia. Al votar el último no-soberano: calcula `favoritas_del_pueblo` (más votadas, con empates) y pasa a DECISION.
  - `void avanzar(String token)`. Solo anfitrión o soberano (si no, `ProhibidoException`). RESPUESTAS→VOTACION (requiere ≥2 respuestas, si no `ConflictoException`); VOTACION→DECISION (cierra con lo que haya, calcula favoritas); RESULTADO→nueva ronda (rota soberano en orden de llegada **con vuelta al primero**, repone manos a 7, repone carta negra, fase RESPUESTAS); otra fase → `ConflictoException`.

- [ ] **Step 1: Write the failing test** — voto válido marca `ya_voto`; soberano vota → `ProhibidoException`; doble voto → `ConflictoException`; votar la propia permitido; empate en favoritas; `avanzar` en RESPUESTAS con <2 → `ConflictoException`; **rotación con vuelta**: con soberano = último de llegada, tras `avanzar` en RESULTADO el soberano pasa a ser el primero.
- [ ] **Step 2–5:** fallo, implementar, PASS, commit `feat(dominio): votar, avanzar y rotacion del soberano`.

---

### Task 9: Decidir (puntuación, historial, fin de partida)

**Files:**
- Modify: `Partida.java` (`decidir`), `Ronda.java` (ganadora)
- Create: `backend/src/main/java/org/democraxssy/dominio/ResumenRonda.java` (numero, soberanoId, ganadorId, frase, respetoAlPueblo)
- Test: `backend/src/test/java/org/democraxssy/dominio/DecidirTest.java`

**Interfaces:**
- Produces: `void decidir(String tokenSoberano, String respuestaId)`. Fase DECISION, solo soberano; la respuesta debe existir (si no, `NoEncontradoException`). Suma **+1** al autor; arma `frase` (carta negra con los `____` reemplazados en orden por las cartas de la respuesta); `respeto_al_pueblo` = ganadora ∈ favoritas **o nadie votó**; agrega `ResumenRonda` al historial; fase → RESULTADO, o → TERMINADA con `ganador_id` si el autor alcanzó `puntos_para_ganar`.

- [ ] **Step 1: Write the failing test** — decidir suma punto al autor y pasa a RESULTADO; alcanzar `puntos_para_ganar` pasa a TERMINADA con `ganador_id`; `respeto_al_pueblo` true cuando la ganadora era favorita; **true cuando nadie votó**; false cuando no era la más votada; no-soberano → `ProhibidoException`.
- [ ] **Step 2–5:** fallo, implementar, PASS, commit `feat(dominio): decidir, puntuacion, historial y fin`.

---

### Task 10: Revelado progresivo (consulta por jugador)

**Files:**
- Create: `backend/src/main/java/org/democraxssy/dominio/Revelado.java` (consultas que respetan la fase)
- Test: `backend/src/test/java/org/democraxssy/dominio/ReveladoTest.java`

**Interfaces:**
- Produces: métodos de consulta sobre `Ronda`/`Partida` que devuelven lo visible **según la fase y el jugador**: respuestas vacías en RESPUESTAS; barajadas y anónimas desde VOTACION; `votos` desde DECISION; `autor` desde RESULTADO; `es_mia` siempre para el autor. La mano solo se expone al propio jugador.

- [ ] **Step 1: Write the failing test** — en RESPUESTAS la lista de respuestas visible está vacía; en VOTACION hay respuestas sin autor ni votos; en DECISION aparecen votos pero no autor; en RESULTADO aparece autor; un jugador nunca ve la mano de otro.
- [ ] **Step 2–5:** fallo, implementar, PASS, commit `feat(dominio): revelado progresivo por fase`.

---

### Task 11: Prueba de integración — partida completa

**Files:**
- Test: `backend/src/test/java/org/democraxssy/dominio/PartidaCompletaTest.java`

**Interfaces:** Consume toda la API de dominio anterior. No crea producción nueva.

- [ ] **Step 1: Write the test** — simula 4 jugadores: crear, 3 se unen, iniciar, responder×3 → VOTACION, votar×3 → DECISION, decidir → RESULTADO, avanzar → nueva ronda con soberano rotado; repetir hasta que alguien llegue a `puntos_para_ganar` y la fase sea TERMINADA con `ganador_id`. Afirma que en ningún punto se filtran manos/votos/autores antes de tiempo y que el mazo nunca se agota (forzar mazo pequeño).
- [ ] **Step 2: Run test** — Expected: PASS (todo ya implementado).
- [ ] **Step 3: Commit** — `git commit -m "test(dominio): partida completa de extremo a extremo"`
