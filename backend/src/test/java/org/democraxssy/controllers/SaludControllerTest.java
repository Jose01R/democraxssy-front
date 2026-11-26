package org.democraxssy.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SaludController.class)
class SaludControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void headLimpioDa200() throws Exception {
        mvc.perform(head("/api/salud")).andExpect(status().isOk());
    }

    @Test
    void headConParametroDeConsultaDa409() throws Exception {
        mvc.perform(head("/api/salud").param("x", "1")).andExpect(status().isConflict());
    }

    @Test
    void headConAuthorizationDa409() throws Exception {
        mvc.perform(head("/api/salud").header("Authorization", "Bearer " + "a".repeat(64)))
                .andExpect(status().isConflict());
    }

    @Test
    void getNoEstaPermitido() throws Exception {
        // El contrato solo define HEAD; GET queda fuera (405), no 200.
        mvc.perform(get("/api/salud")).andExpect(status().isMethodNotAllowed());
    }
}
