#!/usr/bin/env node
// Juega una partida completa de 4 jugadores (meta 1 punto) y la elimina al final.
// Verifica cada fase: unirse, iniciar, respuestas anonimas, votos y favoritas, decision, fin y eliminacion.
//
//   node tools/partida-completa.mjs <url-base>
//   node tools/partida-completa.mjs https://democraxssy-if6000.duckdns.org/api/http/api-democraxssy-if6000.duckdns.org

const base = (process.argv[2] || "").replace(/\/+$/, "");
if (!base) { console.error("Uso: node tools/partida-completa.mjs <url-base>"); process.exit(2); }
async function req(m, ruta, cuerpo, token) {
  const h = {};
  if (cuerpo !== undefined) h["Content-Type"] = "application/json";
  if (token) h.Authorization = `Bearer ${token}`;
  const r = await fetch(base + ruta, { method: m, headers: h, body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo) });
  const t = await r.text();
  return { s: r.status, j: t ? JSON.parse(t) : null };
}
const ok = (c, msg) => { console.log((c ? "OK  " : "XX  ") + msg); if (!c) process.exitCode = 1; };
const c = await req("POST", "/api/partidas", { nombre: "Ana", puntos_para_ganar: 1 });
ok(c.s === 201, `crear ${c.s}`);
const id = c.j.partida_id, P = `/api/partidas/${id}`;
const js = [c.j];
for (const n of ["Beto", "Caro", "Dani"]) { const u = await req("POST", `${P}/jugadores`, { nombre: n }); ok(u.s === 201, `unirse ${n} ${u.s}`); js.push(u.j); }
const i = await req("POST", `${P}/iniciar`, {}, js[0].token);
ok(i.s === 200 && i.j.fase === "respuestas" && i.j.yo.mano.length === 7, `iniciar ${i.s} fase=${i.j?.fase}`);
const sob = i.j.ronda.soberano_id; ok(sob === js[0].jugador_id, "soberano = anfitrion");
const elegir = i.j.ronda.carta_negra.elegir;
let v;
for (const s of js.slice(1)) {
  const g = await req("GET", P, undefined, s.token);
  v = await req("PUT", `${P}/respuestas`, { cartas: g.j.yo.mano.slice(0, elegir).map((x) => x.id) }, s.token);
  ok(v.s === 200, `responder ${v.s}`);
}
ok(v.j.fase === "votacion" && v.j.ronda.respuestas.length === 3 && v.j.ronda.respuestas.every((r) => r.autor_id === undefined && r.votos === undefined), "votacion con 3 respuestas anonimas");
const objetivo = v.j.ronda.respuestas[0].id;
for (const s of js.slice(1)) { v = await req("PUT", `${P}/votos`, { respuesta_id: objetivo }, s.token); ok(v.s === 200, `votar ${v.s}`); }
ok(v.j.fase === "decision" && v.j.ronda.favoritas_del_pueblo[0] === objetivo && v.j.ronda.respuestas.every((r) => r.votos !== undefined), "decision con votos y favoritas");
const d = await req("POST", `${P}/decision`, { respuesta_id: objetivo }, js[0].token);
ok(d.s === 200 && d.j.fase === "terminada" && d.j.ganador_id && d.j.historial[0].respeto_al_pueblo === true, `decidir ${d.s} fase=${d.j?.fase}`);
ok(d.j.ronda.respuestas.every((r) => r.autor_id), "autores revelados al terminar");
const e = await req("DELETE", P, undefined, js[0].token); ok(e.s === 204, `eliminar ${e.s}`);
const g = await req("GET", P); ok(g.s === 404, `GET tras eliminar ${g.s}`);
