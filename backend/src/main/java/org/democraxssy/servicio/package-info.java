/**
 * Logica del juego (la capa "business/service").
 *
 * <p>{@code JuegoServicio} es el motor de estados (crear, unirse, iniciar, responder,
 * votar, avanzar, decidir) mas la puntuacion, el revelado por jugador y el manejo del
 * mazo. Es Java plano, sin dependencias web: en los tests se instancia directo, sin
 * levantar contexto de Spring. Incluye utilidades: {@code Ids}, {@code CargadorMazo}.
 */
package org.democraxssy.servicio;
