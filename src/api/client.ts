// src/api/client.ts
//
// Este archivo es el "corazón" de validadas y demostradas:
//  - Una sola instancia de Axios por la que pasan TODAS las llamadas.
//  - Un registro (log) de cada petición: método, URL, código y tiempo.
//  - El manejo explícito del 409 (el backend lo devuelve ante cualquier
//    campo, parámetro o cabecera de más).
//  - La decisión https directo vs http por proxy /api/.
//
// orval usa la función `customAxios` de abajo como "mutator", así que las
// funciones generadas (crear partida, unirse, votar, etc.) pasan todas por aquí.

import axios from "axios";
import type { AxiosError, AxiosRequestConfig } from "axios";
import { recordCall } from "./callLog";

// --- Backend elegido en tiempo de ejecución (lo fija la pantalla de config) ---
let backendBaseUrl = "";

/** Lo llama la pantalla de configuración cuando el usuario elige servidor. */
export function setBackend(url: string): void {
  backendBaseUrl = url.trim().replace(/\/+$/, ""); // sin slash final
}

export function getBackend(): string {
  return backendBaseUrl;
}

/**
 * Decide cómo se alcanza el backend:
 *  - https://  -> directo desde el navegador (el server del profe tiene CORS abierto).
 *  - http://   -> vía /api/ en nuestro Nginx. Una página HTTPS no puede llamar http://
 *                 directo (contenido mixto, el navegador lo bloquea).
 *
 * IMPORTANTE (coordinar con Joel / infra): cómo /api/ sabe a qué backend http
 * reenviar depende de la config de Nginx. Si el profe prueba contra UN backend
 * http fijo, /api/ apunta a ese host y basta. Si hay que soportar http arbitrario,
 * hay que pasarle el destino a Nginx y aplicar la lista blanca anti-SSRF
 * (nada de localhost, IPs privadas ni 169.254.x.x). Dejar esto definido antes
 * de la entrega.
 */
function resolveBaseUrl(): string {
  if (backendBaseUrl.startsWith("https://")) return backendBaseUrl;
  if (backendBaseUrl.startsWith("http://")) return "/api";
  return backendBaseUrl;
}

// Instancia única. OJO: no le ponemos headers por defecto a propósito.
// Axios ya pone Content-Type: application/json cuando el body es un objeto,
// que es lo que la API espera; cualquier cabecera extra dispararía un 409.
const instance = axios.create();

/**
 * Mutator de orval. Firma esperada: (config, options?) => Promise<T>.
 * Registra cada llamada (éxito o error) y devuelve directamente `res.data`.
 */
export const customAxios = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const started = performance.now();
  const base = resolveBaseUrl();
  const method = (config.method ?? "get").toUpperCase();
  const fullUrl = base + (config.url ?? "");

  return instance({ ...config, ...options, baseURL: base })
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
      const status = error.response?.status ?? 0; // 0 = ni siquiera hubo respuesta
      recordCall({
        method,
        url: fullUrl,
        status,
        ms: Math.round(performance.now() - started),
        ok: false,
      });

      if (status === 409) {
        // El backend rechaza campos/parámetros/cabeceras de más. Lo hacemos visible
        // para que durante el desarrollo sepamos que enviamos algo que sobra.
        console.warn(
          "[409] El backend rechazó la petición: algún campo, parámetro o cabecera de más.",
          { url: fullUrl },
        );
      }
      return Promise.reject(error);
    });
};

export default customAxios;
