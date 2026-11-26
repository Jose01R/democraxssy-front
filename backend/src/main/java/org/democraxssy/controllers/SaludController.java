package org.democraxssy.controllers;

import org.democraxssy.errores.ConflictoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Chequeo de vida para infraestructura.
 *
 * <p>El contrato define unicamente {@code HEAD /api/salud} → 200 sin cuerpo. No lleva
 * cabecera {@code Authorization} ni parametros de consulta; si llegan, es una solicitud
 * inesperada y se responde {@code 409} (regla del Conflicto del contrato).
 *
 * <p>No exponemos {@code GET}: queda fuera del contrato, asi que un GET cae en 405.
 */
@RestController
public class SaludController {

    @RequestMapping(path = "/api/salud", method = RequestMethod.HEAD)
    public ResponseEntity<Void> salud(HttpServletRequest peticion) {
        if (!peticion.getParameterMap().isEmpty()) {
            throw new ConflictoException("La verificacion de salud no admite parametros de consulta.");
        }
        if (peticion.getHeader("Authorization") != null) {
            throw new ConflictoException("La verificacion de salud no admite la cabecera Authorization.");
        }
        return ResponseEntity.ok().build();
    }
}
