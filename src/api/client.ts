// src/api/client.ts
//
// El "corazón" de validadas y demostradas:
//  - Una sola instancia de Axios por la que pasan TODAS las llamadas (mutator de orval).
//  - Registro de cada petición: método, URL, código y tiempo.
//  - Inyección del token Bearer SOLO en las rutas que lo admiten (regla del 409).
//  - Manejo explícito del 409 y decisión https-directo / http-por-proxy.

import axios from "axios";
import type { AxiosError, AxiosRequestConfig } from "axios";
import { recordCall } from "./callLog";

// --- Backend elegido en tiempo de ejecución (lo fija la pantalla de config) ---
let backendBaseUrl = "";

export function setBackend(url: string): void {
  backendBaseUrl = url.trim().replace(/\/+$/, "");
}
export function getBackend(): string {
  return backendBaseUrl;
}

// --- Token de sesión. Por pestaña (sessionStorage): cada pestaña es un jugador. ---
let authToken: string | null = sessionStorage.getItem("token");

export function setAuthToken(token: string | null): void {
  authToken = token;
  if (token) sessionStorage.setItem("token", token);
  else sessionStorage.removeItem("token");
}
export function getAuthToken(): string | null {
  return authToken;
}

// Rutas que NO admiten la cabecera Authorization (el backend la rechaza con 409):
//   - HEAD /api/salud
//   - POST /api/partidas                   (crear)
//   - POST /api/partidas/{id}/jugadores     (unirse)
// El resto (obtener, iniciar, responder, votar, decidir, avanzar, eliminar) sí.
function aceptaAuth(method: string, url: string): boolean {
  const m = (method || "GET").toUpperCase();
  if (m === "HEAD") return false; // salud
  if (m === "POST" && /\/api\/partidas\/?$/.test(url)) return false; // crear
  if (/\/jugadores\/?$/.test(url)) return false; // unirse
  return true;
}

// https:// -> directo (CORS abierto). http:// -> vía /api/ en Nginx (contenido mixto).
// (Coordinar con infra a qué backend http reenvía /api/ y la lista blanca anti-SSRF.)
function resolveBaseUrl(): string {
  if (backendBaseUrl.startsWith("https://")) return backendBaseUrl;
  if (backendBaseUrl.startsWith("http://")) return "/api";
  return backendBaseUrl;
}

// Sin headers por defecto a propósito: cualquier cabecera de más dispara 409.
const instance = axios.create();

export const customAxios = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const started = performance.now();
  const base = resolveBaseUrl();
  const method = (config.method ?? "get").toUpperCase();
  const url = config.url ?? "";
  const fullUrl = base + url;

  // Mezcla de cabeceras + inyección condicional del Bearer.
  const headers: Record<string, unknown> = {
    ...(config.headers as Record<string, unknown> | undefined),
    ...(options?.headers as Record<string, unknown> | undefined),
  };
  if (authToken && aceptaAuth(method, url)) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  return instance({ ...config, ...options, baseURL: base, headers })
    .then((res) => {
      recordCall({
        method,
        url: fullUrl,
        status: res.status,
        ms: Math.round(performance.now() - started),
        ok: true,
      });
      return res.data as T;
    })
    .catch((error: AxiosError) => {
      const status = error.response?.status ?? 0;
      recordCall({
        method,
        url: fullUrl,
        status,
        ms: Math.round(performance.now() - started),
        ok: false,
      });
      if (status === 409) {
        console.warn(
          "[409] El backend rechazó la petición: campo, parámetro o cabecera de más, o acción fuera de fase.",
          { url: fullUrl },
        );
      }
      return Promise.reject(error);
    });
};

export default customAxios;
