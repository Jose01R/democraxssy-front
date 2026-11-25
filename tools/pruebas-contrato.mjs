#!/usr/bin/env node
// Pruebas del contrato (openapi.yaml) contra un servidor de Cards for democraXSSy.
//
//   node tools/pruebas-contrato.mjs <url-base>
//   node tools/pruebas-contrato.mjs --comparar <url-A> <url-B>
//   node tools/pruebas-contrato.mjs <url-base> --json resultado.json
//
// Ejemplos:
//   node tools/pruebas-contrato.mjs https://democraxssy.akamai.meseguercr.com
//   node tools/pruebas-contrato.mjs https://api-democraxssy-if6000.duckdns.org
//   node tools/pruebas-contrato.mjs --comparar https://democraxssy.akamai.meseguercr.com https://api-democraxssy-if6000.duckdns.org
//   # Backend HTTP a traves del proxy del frontend:
//   node tools/pruebas-contrato.mjs https://democraxssy-if6000.duckdns.org/api/http/api-democraxssy-if6000.duckdns.org
//
// Usa node:http/https (no fetch) para poder enviar lo que el contrato exige rechazar:
// cuerpo en GET, cabeceras repetidas y JSON con claves duplicadas.
// Crea partidas reales: la del flujo completo se elimina al final; las de validacion quedan en "sala".

import http from "node:http";
import https from "node:https";
import { writeFileSync } from "node:fs";

// ---------- HTTP ----------
function solicitud(base, metodo, ruta, { cabeceras = {}, cuerpo = null } = {}) {
  const url = new URL(base.replace(/\/+$/, "") + ruta);
  const lib = url.protocol === "https:" ? https : http;
  const h = { ...cabeceras };
  if (cuerpo !== null) h["Content-Length"] = Buffer.byteLength(cuerpo);
  const inicio = Date.now();
  return new Promise((resolver) => {
    const r = lib.request(url, { method: metodo, headers: h, timeout: 20000 }, (resp) => {
      let datos = "";
      resp.setEncoding("utf8");
      resp.on("data", (c) => (datos += c));
      resp.on("end", () => {
        let json = null;
        try { json = datos ? JSON.parse(datos) : null; } catch { /* no es JSON */ }
        resolver({ estado: resp.statusCode, cabeceras: resp.headers, texto: datos, json, ms: Date.now() - inicio });
      });
    });
    r.on("timeout", () => r.destroy(new Error("tiempo agotado")));
    r.on("error", (e) => resolver({ estado: 0, cabeceras: {}, texto: String(e.message), json: null, ms: Date.now() - inicio }));
    if (cuerpo !== null) r.write(cuerpo);
    r.end();
  });
}
const JSON_CT = { "Content-Type": "application/json" };
const bearer = (t) => ({ Authorization: `Bearer ${t}` });
const enviar = (base, metodo, ruta, obj, token, extra = {}) =>
  solicitud(base, metodo, ruta, {
    cabeceras: { ...JSON_CT, ...(token ? bearer(token) : {}), ...extra },
    cuerpo: typeof obj === "string" ? obj : JSON.stringify(obj),
  });
const leer = (base, ruta, token) => solicitud(base, "GET", ruta, { cabeceras: token ? bearer(token) : {} });

// ---------- Validacion de forma (segun openapi.yaml) ----------
const hex = (n) => new RegExp(`^[0-9a-f]{${n}}$`);
const FASES = ["sala", "respuestas", "votacion", "decision", "resultado", "terminada"];
function faltan(obj, claves, donde, err) {
  if (obj === null || typeof obj !== "object" || Array.isArray(obj)) { err.push(`${donde}: no es un objeto`); return false; }
  for (const c of claves) if (!(c in obj)) err.push(`${donde}: falta "${c}"`);
  return true;
}
function formaVista(v, donde = "vista") {
  const e = [];
  if (!faltan(v, ["id", "fase", "anfitrion_id", "puntos_para_ganar", "jugadores", "ronda", "yo", "historial", "ganador_id"], donde, e)) return e;
  if (!hex(32).test(v.id)) e.push(`${donde}.id no es de 32 hex`);
  if (!FASES.includes(v.fase)) e.push(`${donde}.fase invalida: ${v.fase}`);
  if (!hex(16).test(v.anfitrion_id)) e.push(`${donde}.anfitrion_id no es de 16 hex`);
  if (!Number.isInteger(v.puntos_para_ganar)) e.push(`${donde}.puntos_para_ganar no es entero`);
  if (!Array.isArray(v.jugadores)) e.push(`${donde}.jugadores no es arreglo`);
  else v.jugadores.forEach((j, i) => faltan(j, ["id", "nombre", "puntos", "es_anfitrion", "es_soberano", "ya_respondio", "ya_voto"], `${donde}.jugadores[${i}]`, e));
  if (v.ronda !== null) {
    if (faltan(v.ronda, ["numero", "soberano_id", "carta_negra", "respuestas_recibidas", "votos_emitidos", "respuestas", "favoritas_del_pueblo", "ganadora"], `${donde}.ronda`, e)) {
      faltan(v.ronda.carta_negra, ["texto", "elegir"], `${donde}.ronda.carta_negra`, e);
      (v.ronda.respuestas || []).forEach((r, i) => {
        faltan(r, ["id", "cartas", "frase", "es_mia"], `${donde}.ronda.respuestas[${i}]`, e);
        if (r.id && !hex(12).test(r.id)) e.push(`${donde}.ronda.respuestas[${i}].id no es de 12 hex`);
      });
    }
  }
  if (v.yo !== null && faltan(v.yo, ["id", "nombre", "mano", "mi_voto", "puede_avanzar"], `${donde}.yo`, e)) {
    if (!Array.isArray(v.yo.mano) || v.yo.mano.length > 7) e.push(`${donde}.yo.mano invalida`);
  }
  if (!Array.isArray(v.historial)) e.push(`${donde}.historial no es arreglo`);
  else v.historial.forEach((h, i) => faltan(h, ["numero", "soberano_id", "ganador_id", "frase", "respeto_al_pueblo"], `${donde}.historial[${i}]`, e));
  return e;
}
function formaSesion(s) {
  const e = [];
  if (!faltan(s, ["partida_id", "jugador_id", "token", "partida"], "sesion", e)) return e;
  if (!hex(32).test(s.partida_id)) e.push("sesion.partida_id no es de 32 hex");
  if (!hex(16).test(s.jugador_id)) e.push("sesion.jugador_id no es de 16 hex");
  if (!hex(64).test(s.token)) e.push("sesion.token no es de 64 hex");
  return e.concat(formaVista(s.partida, "sesion.partida"));
}
const formaError = (r) => (r.json && typeof r.json.error === "string" ? [] : ["el cuerpo no es {\"error\": \"...\"}"]);

// ---------- Registro de casos ----------
function crearSuite(base) {
  const casos = [];
  const caso = async (id, descripcion, esperado, fn) => {
    let r, notas = [];
    try { ({ r, notas = [] } = await fn()); }
    catch (ex) { r = { estado: 0, cabeceras: {}, ms: 0 }; notas = [`excepcion: ${ex.message}`]; }
    const cors = r.cabeceras?.["access-control-allow-origin"] === "*";
    if (r.estado && !cors) notas.push("falta Access-Control-Allow-Origin: *");
    const okEstado = esperado === null || (Array.isArray(esperado) ? esperado.includes(r.estado) : r.estado === esperado);
    casos.push({ id, descripcion, esperado, estado: r.estado, ok: okEstado && notas.length === 0, notas, ms: r.ms, error: r.json?.error });
    return r;
  };
  return { casos, caso, base };
}

// ---------- Pruebas ----------
async function correr(base) {
  const S = crearSuite(base);
  const { caso } = S;
  const P = "/api/partidas";
  const nombre = (n) => `${n}${Math.floor(Math.random() * 1e4)}`;

  // Servicio y CORS
  await caso("salud", "HEAD /api/salud", 200, async () => ({ r: await solicitud(base, "HEAD", "/api/salud") }));
  await caso("preflight", "OPTIONS (preflight CORS)", null, async () => {
    const r = await solicitud(base, "OPTIONS", P, { cabeceras: { Origin: "https://ejemplo.com", "Access-Control-Request-Method": "POST", "Access-Control-Request-Headers": "authorization,content-type" } });
    const notas = [];
    const met = String(r.cabeceras["access-control-allow-methods"] || "").toUpperCase();
    const cab = String(r.cabeceras["access-control-allow-headers"] || "").toLowerCase();
    for (const m of ["GET", "HEAD", "POST", "PUT", "DELETE"]) if (!met.includes(m)) notas.push(`Allow-Methods sin ${m}`);
    for (const c of ["authorization", "content-type"]) if (!cab.includes(c) && cab !== "*") notas.push(`Allow-Headers sin ${c}`);
    if (r.estado < 200 || r.estado > 299) notas.push(`estado ${r.estado} no es 2xx`);
    return { r, notas };
  });

  // Validacion previa (409)
  await caso("v-query", "Parametro de consulta (?x=1)", 409, async () => ({ r: await solicitud(base, "HEAD", "/api/salud?x=1") }));
  await caso("v-auth-publica", "Authorization en /api/salud", 409, async () => ({ r: await solicitud(base, "HEAD", "/api/salud", { cabeceras: bearer("a".repeat(64)) }) }));
  await caso("v-auth-crear", "Authorization al crear partida", 409, async () => { const r = await enviar(base, "POST", P, { nombre: "Ana" }, "a".repeat(64)); return { r, notas: formaError(r) }; });
  await caso("v-ct", "Content-Type text/plain al crear", 409, async () => { const r = await solicitud(base, "POST", P, { cabeceras: { "Content-Type": "text/plain" }, cuerpo: '{"nombre":"Ana"}' }); return { r, notas: formaError(r) }; });
  await caso("v-ct-charset", "Content-Type con ; charset=utf-8 (valido)", 201, async () => { const r = await solicitud(base, "POST", P, { cabeceras: { "Content-Type": "application/json; charset=utf-8" }, cuerpo: JSON.stringify({ nombre: "Ana" }) }); return { r }; });
  await caso("v-override", "X-HTTP-Method-Override", 409, async () => ({ r: await enviar(base, "POST", P, { nombre: "Ana" }, null, { "X-HTTP-Method-Override": "DELETE" }) }));
  await caso("v-desconocido", "JSON con campo desconocido", 409, async () => ({ r: await enviar(base, "POST", P, { nombre: "Ana", admin: true }) }));
  await caso("v-repetido", "JSON con clave repetida", 409, async () => ({ r: await enviar(base, "POST", P, '{"nombre":"Ana","nombre":"Beto"}') }));
  await caso("v-falta", "JSON sin el campo obligatorio", 409, async () => ({ r: await enviar(base, "POST", P, {}) }));
  await caso("v-tipo", "JSON con tipo incorrecto (nombre: 5)", 409, async () => ({ r: await enviar(base, "POST", P, { nombre: 5 }) }));
  await caso("v-no-objeto", "JSON que no es objeto ([])", 409, async () => ({ r: await enviar(base, "POST", P, "[]") }));
  await caso("v-mal-json", "JSON mal formado", 409, async () => ({ r: await enviar(base, "POST", P, '{"nombre":') }));

  // Datos invalidos (400) y tamano (413)
  await caso("d-vacio", "Nombre solo con espacios", 400, async () => { const r = await enviar(base, "POST", P, { nombre: "   " }); return { r, notas: formaError(r) }; });
  await caso("d-largo", "Nombre de 25 caracteres", 400, async () => ({ r: await enviar(base, "POST", P, { nombre: "x".repeat(25) }) }));
  await caso("d-control", "Nombre con caracter de control", 400, async () => ({ r: await enviar(base, "POST", P, { nombre: "Ana\u0007" }) }));
  await caso("d-puntos-0", "puntos_para_ganar = 0", 400, async () => ({ r: await enviar(base, "POST", P, { nombre: "Ana", puntos_para_ganar: 0 }) }));
  await caso("d-puntos-21", "puntos_para_ganar = 21", 400, async () => ({ r: await enviar(base, "POST", P, { nombre: "Ana", puntos_para_ganar: 21 }) }));
  await caso("d-puntos-null", "puntos_para_ganar = null (usa 5)", 201, async () => { const r = await enviar(base, "POST", P, { nombre: "Ana", puntos_para_ganar: null }); return { r, notas: r.json?.partida?.puntos_para_ganar === 5 ? [] : ["no quedo en 5"] }; });
  await caso("d-413", "Cuerpo de mas de 4 KB", 413, async () => ({ r: await enviar(base, "POST", P, { nombre: "Ana", relleno: "x".repeat(5000) }) }));

  // Rutas y recursos
  await caso("r-404-ruta", "Ruta inexistente", 404, async () => ({ r: await leer(base, "/api/no-existe") }));
  await caso("r-404-partida", "Partida inexistente (32 hex)", 404, async () => ({ r: await leer(base, `${P}/${"0".repeat(32)}`) }));
  await caso("r-id-mal", "ID de partida mal formado (informativo)", null, async () => ({ r: await leer(base, `${P}/xyz`) }));

  // ----- Flujo completo: 4 jugadores, 1 punto para ganar -----
  const A = await caso("f-crear", "Crear partida (anfitrion)", 201, async () => { const r = await enviar(base, "POST", P, { nombre: nombre("Ana"), puntos_para_ganar: 1 }); return { r, notas: r.estado === 201 ? formaSesion(r.json) : [] }; });
  if (A.estado !== 201) return S.casos;
  const id = A.json.partida_id, PP = `${P}/${id}`;
  const jug = [{ nombre: "Ana", token: A.json.token, id: A.json.jugador_id }];
  for (const n of ["Beto", "Caro", "Dani"]) {
    const r = await caso(`f-unirse-${n}`, `Unirse (${n})`, 201, async () => { const r = await enviar(base, "POST", `${PP}/jugadores`, { nombre: nombre(n) }); return { r, notas: r.estado === 201 ? formaSesion(r.json) : [] }; });
    if (r.estado === 201) jug.push({ nombre: r.json.partida.jugadores.at(-1)?.nombre, token: r.json.token, id: r.json.jugador_id });
  }
  await caso("f-nombre-repetido", "Unirse con nombre repetido (mayusculas)", 409, async () => {
    const v = (await leer(base, PP)).json; const ya = v?.jugadores?.[1]?.nombre || "Beto";
    return { r: await enviar(base, "POST", `${PP}/jugadores`, { nombre: ya.toUpperCase() }) };
  });
  await caso("f-auth-unirse", "Authorization al unirse", 409, async () => ({ r: await enviar(base, "POST", `${PP}/jugadores`, { nombre: "Eva" }, jug[0].token) }));
  await caso("f-vista-publica", "GET sin token (vista publica, yo = null)", 200, async () => { const r = await leer(base, PP); const n = formaVista(r.json); if (r.json?.yo !== null) n.push("yo no es null"); return { r, notas: n }; });
  await caso("f-vista-propia", "GET con token (incluye yo)", 200, async () => { const r = await leer(base, PP, jug[0].token); const n = formaVista(r.json); if (!r.json?.yo) n.push("yo es null"); return { r, notas: n }; });
  await caso("f-token-ajeno", "GET con token bien formado pero ajeno", 401, async () => ({ r: await leer(base, PP, "b".repeat(64)) }));
  await caso("f-token-malo", "GET con token mal formado", 409, async () => ({ r: await leer(base, PP, "NO-ES-HEX") }));
  await caso("f-auth-doble", "Authorization repetida", 409, async () => ({ r: await solicitud(base, "GET", PP, { cabeceras: { Authorization: [`Bearer ${jug[0].token}`, `Bearer ${jug[0].token}`] } }) }));
  await caso("f-get-cuerpo", "GET con cuerpo y Content-Type", 409, async () => ({ r: await solicitud(base, "GET", PP, { cabeceras: JSON_CT, cuerpo: "{}" }) }));
  await caso("f-iniciar-no-anfitrion", "Iniciar sin ser anfitrion", 403, async () => ({ r: await enviar(base, "POST", `${PP}/iniciar`, {}, jug[1].token) }));
  await caso("f-avanzar-sala", "Avanzar en fase sala", 409, async () => ({ r: await enviar(base, "POST", `${PP}/avanzar`, {}, jug[0].token) }));
  await caso("f-iniciar-campo", "Iniciar con campo en el cuerpo", 409, async () => ({ r: await enviar(base, "POST", `${PP}/iniciar`, { x: 1 }, jug[0].token) }));
  const ini = await caso("f-iniciar", "Iniciar (anfitrion, 4 jugadores)", 200, async () => { const r = await enviar(base, "POST", `${PP}/iniciar`, {}, jug[0].token); const n = formaVista(r.json); if (r.json?.fase !== "respuestas") n.push(`fase ${r.json?.fase}`); if (r.json?.yo?.mano?.length !== 7) n.push("la mano no tiene 7 cartas"); return { r, notas: n }; });
  if (ini.estado !== 200) return S.casos;
  await caso("f-unirse-tarde", "Unirse despues de iniciar", 409, async () => ({ r: await enviar(base, "POST", `${PP}/jugadores`, { nombre: nombre("Tarde") }) }));

  const ronda = ini.json.ronda, sob = ronda.soberano_id, elegir = ronda.carta_negra.elegir;
  const soberano = jug.find((j) => j.id === sob);
  const pueblo = jug.filter((j) => j.id !== sob);
  const mano = async (j) => (await leer(base, PP, j.token)).json.yo.mano.map((c) => c.id);

  await caso("f-responde-soberano", "El soberano intenta responder", 403, async () => ({ r: await enviar(base, "PUT", `${PP}/respuestas`, { cartas: (await mano(soberano)).slice(0, elegir) }, soberano.token) }));
  await caso("f-responde-cantidad", "Responder con cantidad incorrecta de cartas", 400, async () => { const m = await mano(pueblo[0]); return { r: await enviar(base, "PUT", `${PP}/respuestas`, { cartas: elegir === 1 ? m.slice(0, 2) : m.slice(0, 1) }, pueblo[0].token) }; });
  await caso("f-responde-ajena", "Responder con carta que no esta en la mano", 400, async () => { const m = await mano(pueblo[0]); const fuera = Math.max(...m) + 100000; return { r: await enviar(base, "PUT", `${PP}/respuestas`, { cartas: elegir === 1 ? [fuera] : [m[0], fuera] }, pueblo[0].token) }; });
  await caso("f-votar-temprano", "Votar en fase respuestas", 409, async () => ({ r: await enviar(base, "PUT", `${PP}/votos`, { respuesta_id: "0".repeat(12) }, pueblo[0].token) }));
  for (const [i, j] of pueblo.entries()) {
    await caso(`f-responder-${i + 1}`, `Responder (jugador ${i + 1} de ${pueblo.length})`, 200, async () => ({ r: await enviar(base, "PUT", `${PP}/respuestas`, { cartas: (await mano(j)).slice(0, elegir) }, j.token) }));
  }
  await caso("f-votacion", "Tras responder todos: fase votacion, respuestas anonimas", 200, async () => {
    const r = await leer(base, PP, pueblo[0].token); const n = formaVista(r.json); const rs = r.json?.ronda?.respuestas || [];
    if (r.json?.fase !== "votacion") n.push(`fase ${r.json?.fase}`);
    if (rs.length !== pueblo.length) n.push(`${rs.length} respuestas reveladas`);
    if (rs.some((x) => "autor_id" in x)) n.push("autor_id revelado antes de resultado");
    if (rs.some((x) => "votos" in x)) n.push("votos revelados antes de decision");
    if (rs.filter((x) => x.es_mia).length !== 1) n.push("es_mia no marca exactamente una");
    return { r, notas: n };
  });
  const rs = (await leer(base, PP, pueblo[0].token)).json.ronda.respuestas;
  const favorita = rs[0].id;
  await caso("f-vota-soberano", "El soberano intenta votar", 403, async () => ({ r: await enviar(base, "PUT", `${PP}/votos`, { respuesta_id: favorita }, soberano.token) }));
  await caso("f-vota-inexistente", "Votar por respuesta inexistente", 404, async () => ({ r: await enviar(base, "PUT", `${PP}/votos`, { respuesta_id: "0".repeat(12) }, pueblo[0].token) }));
  await caso("f-vota-1", "Votar (jugador 1)", 200, async () => ({ r: await enviar(base, "PUT", `${PP}/votos`, { respuesta_id: favorita }, pueblo[0].token) }));
  await caso("f-vota-doble", "Votar dos veces", 409, async () => ({ r: await enviar(base, "PUT", `${PP}/votos`, { respuesta_id: favorita }, pueblo[0].token) }));
  for (const [i, j] of pueblo.slice(1).entries()) await caso(`f-vota-${i + 2}`, `Votar (jugador ${i + 2})`, 200, async () => ({ r: await enviar(base, "PUT", `${PP}/votos`, { respuesta_id: favorita }, j.token) }));
  await caso("f-decision-fase", "Tras votar todos: fase decision, votos y favoritas", 200, async () => {
    const r = await leer(base, PP, soberano.token); const n = formaVista(r.json); const ro = r.json?.ronda;
    if (r.json?.fase !== "decision") n.push(`fase ${r.json?.fase}`);
    if (!ro?.respuestas?.every((x) => Number.isInteger(x.votos))) n.push("faltan votos por respuesta");
    if (!ro?.favoritas_del_pueblo?.includes(favorita)) n.push("favoritas_del_pueblo no incluye la mas votada");
    return { r, notas: n };
  });
  await caso("f-decide-pueblo", "Decidir sin ser soberano", 403, async () => ({ r: await enviar(base, "POST", `${PP}/decision`, { respuesta_id: favorita }, pueblo[0].token) }));
  await caso("f-decidir", "El soberano decide (1 punto: termina)", 200, async () => {
    const r = await enviar(base, "POST", `${PP}/decision`, { respuesta_id: favorita }, soberano.token); const n = formaVista(r.json);
    if (r.json?.fase !== "terminada") n.push(`fase ${r.json?.fase}`);
    if (!r.json?.ganador_id) n.push("ganador_id vacio");
    if (r.json?.historial?.[0]?.respeto_al_pueblo !== true) n.push("respeto_al_pueblo no es true");
    if (!r.json?.ronda?.respuestas?.every((x) => "autor_id" in x)) n.push("autor_id no revelado");
    return { r, notas: n };
  });
  await caso("f-avanzar-fin", "Avanzar en partida terminada", 409, async () => ({ r: await enviar(base, "POST", `${PP}/avanzar`, {}, jug[0].token) }));
  await caso("f-borrar-no-anfitrion", "Eliminar sin ser anfitrion", 403, async () => ({ r: await solicitud(base, "DELETE", PP, { cabeceras: bearer(jug[1].token) }) }));
  await caso("f-borrar", "Eliminar partida terminada (anfitrion)", 204, async () => ({ r: await solicitud(base, "DELETE", PP, { cabeceras: bearer(jug[0].token) }) }));
  await caso("f-borrada", "GET de la partida eliminada", 404, async () => ({ r: await leer(base, PP) }));
  return S.casos;
}

// ---------- Salida ----------
const marca = (c) => (c.ok ? "OK " : "XX ");
function imprimir(base, casos) {
  console.log(`\nServidor: ${base}`);
  console.log("-".repeat(100));
  for (const c of casos) {
    const esp = c.esperado === null ? "info" : Array.isArray(c.esperado) ? c.esperado.join("/") : c.esperado;
    console.log(`${marca(c)} ${String(c.estado).padEnd(4)} (esperado ${String(esp).padEnd(4)}) ${c.descripcion}${c.notas.length ? "\n        - " + c.notas.join("\n        - ") : ""}`);
  }
  const ok = casos.filter((c) => c.ok).length;
  console.log("-".repeat(100));
  console.log(`${ok}/${casos.length} casos correctos`);
}
function comparar(a, b, ca, cb) {
  console.log(`\nComparacion\n  A: ${a}\n  B: ${b}`);
  console.log("-".repeat(100));
  const mapa = new Map(cb.map((c) => [c.id, c]));
  let dif = 0;
  for (const x of ca) {
    const y = mapa.get(x.id);
    const igual = y && x.estado === y.estado && x.ok === y.ok;
    if (!igual) dif++;
    console.log(`${igual ? "== " : "!= "} A ${String(x.estado).padEnd(4)} B ${String(y?.estado ?? "-").padEnd(4)} ${x.descripcion}${!igual && y?.notas?.length ? "  [B: " + y.notas.join("; ") + "]" : ""}`);
  }
  for (const y of cb) if (!ca.find((x) => x.id === y.id)) { dif++; console.log(`!= solo en B: ${y.descripcion} (${y.estado})`); }
  console.log("-".repeat(100));
  console.log(dif ? `${dif} diferencias` : "Sin diferencias");
}

// ---------- Main ----------
const args = process.argv.slice(2);
const iJson = args.indexOf("--json");
const salidaJson = iJson >= 0 ? args.splice(iJson, 2)[1] : null;
if (args[0] === "--comparar" && args.length === 3) {
  const [a, b] = [args[1], args[2]];
  const ca = await correr(a); imprimir(a, ca);
  const cb = await correr(b); imprimir(b, cb);
  comparar(a, b, ca, cb);
  if (salidaJson) writeFileSync(salidaJson, JSON.stringify({ a, b, ca, cb }, null, 2));
} else if (args.length === 1) {
  const casos = await correr(args[0]); imprimir(args[0], casos);
  if (salidaJson) writeFileSync(salidaJson, JSON.stringify({ servidor: args[0], fecha: new Date().toISOString(), casos }, null, 2));
  process.exitCode = casos.every((c) => c.ok) ? 0 : 1;
} else {
  console.log("Uso:\n  node tools/pruebas-contrato.mjs <url-base> [--json salida.json]\n  node tools/pruebas-contrato.mjs --comparar <url-A> <url-B> [--json salida.json]");
  process.exitCode = 2;
}
