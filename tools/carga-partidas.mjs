#!/usr/bin/env node
// Abre muchas partidas a la vez para medir la memoria del backend (fase E).
//
//   node tools/carga-partidas.mjs <url-base> [--partidas 20] [--jugadores 22] [--ritmo 12] [--sin-iniciar]
//
// Cada partida se llena con --jugadores (par, de 4 a 22) y se inicia, asi cada jugador tiene su mano de 7 cartas.
// Las partidas quedan vivas: el contrato solo deja eliminar partidas terminadas, asi que se van con la
// limpieza de inactivas del backend o al reiniciar el servicio.
// --ritmo limita las solicitudes por segundo: el Nginx del backend permite 20/s por IP.

import http from "node:http";
import https from "node:https";

const args = process.argv.slice(2);
const base = (args[0]?.startsWith("--") ? "" : args[0] || "").replace(/\/+$/, "");
const opcion = (nombre, porDefecto) => {
  const i = args.indexOf(`--${nombre}`);
  return i >= 0 ? args[i + 1] : porDefecto;
};
if (!base) {
  console.error("Uso: node tools/carga-partidas.mjs <url-base> [--partidas N] [--jugadores M] [--ritmo R] [--sin-iniciar]");
  process.exit(2);
}

const ritmo = Number(opcion("ritmo", 12));
let ultimo = 0;
async function turno() {
  const espera = ultimo + 1000 / ritmo - Date.now();
  ultimo = Math.max(Date.now(), ultimo + 1000 / ritmo);
  if (espera > 0) await new Promise((r) => setTimeout(r, espera));
}

function solicitud(metodo, ruta, cuerpo = null, token = null) {
  const url = new URL(base + ruta);
  const lib = url.protocol === "https:" ? https : http;
  const h = {};
  if (cuerpo !== null) {
    h["Content-Type"] = "application/json";
    h["Content-Length"] = Buffer.byteLength(cuerpo);
  }
  if (token) h.Authorization = `Bearer ${token}`;
  return new Promise((resolver) => {
    const r = lib.request(url, { method: metodo, headers: h, timeout: 20000 }, (resp) => {
      let datos = "";
      resp.setEncoding("utf8");
      resp.on("data", (c) => (datos += c));
      resp.on("end", () => {
        let json = null;
        try { json = datos ? JSON.parse(datos) : null; } catch { /* no es JSON */ }
        resolver({ estado: resp.statusCode, json });
      });
    });
    r.on("timeout", () => r.destroy(new Error("timeout")));
    r.on("error", () => resolver({ estado: 0, json: null }));
    if (cuerpo !== null) r.write(cuerpo);
    r.end();
  });
}

const conteo = {};
async function llamar(metodo, ruta, objeto = null, token = null) {
  const cuerpo = objeto === null ? null : JSON.stringify(objeto);
  for (let intento = 0; intento < 4; intento++) {
    await turno();
    const r = await solicitud(metodo, ruta, cuerpo, token);
    if (r.estado !== 503 && r.estado !== 429 && r.estado !== 0) {
      conteo[r.estado] = (conteo[r.estado] || 0) + 1;
      return r;
    }
    await new Promise((res) => setTimeout(res, 1000 * (intento + 1)));
  }
  conteo.fallidas = (conteo.fallidas || 0) + 1;
  return { estado: 0, json: null };
}

const inicio = Date.now();
const totalPartidas = Number(opcion("partidas", 20));
const porPartida = Number(opcion("jugadores", 22));
const iniciar = !args.includes("--sin-iniciar");
if (porPartida < 4 || porPartida > 22 || porPartida % 2 !== 0) {
  console.error("--jugadores debe ser par, de 4 a 22");
  process.exit(2);
}

const creadas = [];
let unidos = 0;
let iniciadas = 0;
for (let i = 0; i < totalPartidas; i++) {
  const c = await llamar("POST", "/api/partidas", { nombre: `Carga${i}-0`, puntos_para_ganar: 20 });
  if (c.estado !== 201) continue;
  const p = { id: c.json.partida_id, token: c.json.token };
  creadas.push(p);
  let enSala = 1;
  for (let j = 1; j < porPartida; j++) {
    const u = await llamar("POST", `/api/partidas/${p.id}/jugadores`, { nombre: `Carga${i}-${j}` });
    if (u.estado === 201) { unidos++; enSala++; }
  }
  if (iniciar && enSala === porPartida) {
    const r = await llamar("POST", `/api/partidas/${p.id}/iniciar`, {}, p.token);
    if (r.estado === 200) iniciadas++;
  }
  process.stderr.write(`\rPartidas: ${creadas.length}/${totalPartidas}`);
}
process.stderr.write("\n");

console.log(JSON.stringify({
  accion: "cargar", partidas: creadas.length, jugadores: creadas.length + unidos, iniciadas,
  codigos: conteo, segundos: Math.round((Date.now() - inicio) / 1000),
}));
