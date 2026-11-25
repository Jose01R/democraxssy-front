package org.democraxssy.web;

import org.democraxssy.errores.ConflictoException;
import org.democraxssy.errores.DatosInvalidosException;
import org.democraxssy.errores.NoAutenticadoException;
import org.democraxssy.errores.NoEncontradoException;
import org.democraxssy.errores.ProhibidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(DatosInvalidosException.class)
    ResponseEntity<ErrorRespuesta> datosInvalidos(DatosInvalidosException e) {
        return responder(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    ResponseEntity<ErrorRespuesta> noAutenticado(NoAutenticadoException e) {
        return responder(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(ProhibidoException.class)
    ResponseEntity<ErrorRespuesta> prohibido(ProhibidoException e) {
        return responder(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(NoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ErrorRespuesta> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorRespuesta("Cuerpo JSON invalido"));
    }

    private static ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, RuntimeException e) {
        return ResponseEntity.status(estado).body(new ErrorRespuesta(e.getMessage()));
    }
}
