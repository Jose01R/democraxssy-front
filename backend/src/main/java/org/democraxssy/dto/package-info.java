/**
 * DTOs del contrato ({@code openapi.yaml}), paquete neutral compartido.
 *
 * <p>Respuestas: Sesion, Vista, JugadorPublico, Yo, Ronda, RespuestaRevelada, ResumenRonda.
 * Son {@code record}s planos que {@link org.democraxssy.servicio.JuegoServicio} devuelve ya
 * filtrados por jugador (revelado progresivo) y que Jackson serializa directo al JSON del
 * contrato: por eso viven en un paquete neutral (no en {@code web}), para que el servicio los
 * use sin depender de la capa web. No hace falta una capa de mapeo.
 */
package org.democraxssy.dto;
