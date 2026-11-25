package org.democraxssy.salud;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chequeo de vida para infraestructura.
 *
 * <p>El contrato define {@code HEAD /api/salud}. Respondemos 200 sin cuerpo.
 * Aceptamos tambien GET por comodidad al probar con el navegador; ambos
 * devuelven lo mismo (200, sin cuerpo).
 */
@RestController
public class SaludController {

    @RequestMapping(path = "/api/salud", method = { RequestMethod.HEAD, RequestMethod.GET })
    public ResponseEntity<Void> salud() {
        return ResponseEntity.ok().build();
    }
}
