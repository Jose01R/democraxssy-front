/**
 * Plomeria web transversal.
 *
 * <p>Filtro CORS (en todas las respuestas, incluidas las de error) y el manejador global
 * ({@code ManejadorErrores}, {@code @RestControllerAdvice}) que convierte las excepciones de
 * {@link org.democraxssy.errores} en el formato {@code {"error": ...}} del contrato. Los DTOs
 * viven en {@link org.democraxssy.dto} (paquete neutral); no hay capa de mapeo. Se llena en la
 * Fase C/D.
 */
package org.democraxssy.web;
