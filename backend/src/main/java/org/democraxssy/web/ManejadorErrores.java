package org.democraxssy.web;

import java.util.Locale;

import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.DatosInvalidosException;
import org.democraxssy.errores.NoAutenticadoException;
import org.democraxssy.errores.NoEncontradoException;
import org.democraxssy.errores.ProhibidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Las excepciones propias de Spring MVC (metodo no soportado, Content-Type, ruta inexistente...) las resuelve
 * ResponseEntityExceptionHandler con su codigo estandar; aqui solo se cambia el cuerpo al formato {"error": "..."}.
 * Para mapear alguna a 409 se sobrescribe su metodo handleXxx, como el del cuerpo JSON ilegible. El handler de
 * Exception es el ultimo recurso: solo atrapa lo inesperado y responde 500 sin detalles.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(DatosInvalidosException.class)
    ResponseEntity<ErrorRespuesta> datosInvalidos(DatosInvalidosException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoAutenticadoException.class)
    ResponseEntity<ErrorRespuesta> noAutenticado(NoAutenticadoException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ProhibidoException.class)
    ResponseEntity<ErrorRespuesta> prohibido(ProhibidoException e) {
        return responder(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ErrorRespuesta> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    /** El contrato trata un cuerpo JSON mal formado o con claves repetidas como inesperado: 409. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
        String mensaje = esClaveRepetida(e)
                ? "Cuerpo JSON invalido o inesperado: clave repetida."
                : "Cuerpo JSON invalido o inesperado.";
        return ResponseEntity.status(HttpStatus.CONFLICT).headers(cabeceras).body(new ErrorRespuesta(mensaje));
    }

    /**
     * Jackson no tiene una excepcion propia para la deteccion estricta de duplicados: reporta un
     * error de parseo cuyo mensaje contiene "Duplicate". Se recorre la cadena de causas buscandolo.
     */
    private static boolean esClaveRepetida(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String mensaje = t.getMessage();
            if (mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains("duplicate")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object cuerpo, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
        return ResponseEntity.status(estado).headers(cabeceras).body(new ErrorRespuesta(mensaje(estado)));
    }

    private static String mensaje(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 400 -> "Solicitud invalida";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Metodo no permitido";
            case 406 -> "Formato de respuesta no aceptable";
            case 413 -> "Cuerpo demasiado grande";
            case 415 -> "Content-Type no soportado";
            default -> estado.is5xxServerError() ? "Error interno del servidor" : "Solicitud rechazada";
        };
    }

    private static ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorRespuesta(mensaje));
    }
}
