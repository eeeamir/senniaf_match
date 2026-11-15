package com.senniaf.match.controller;

import com.senniaf.match.service.VinculacionService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VinculacionControllerTest {
    VinculacionService service=mock(VinculacionService.class);
    MockMvc mvc=MockMvcBuilders.standaloneSetup(new VinculacionController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    @Test void profesionalesNoPuedenResponderPorFamilia() throws Exception {
        for(String rol:new String[]{"COMITE","TRABAJADOR_SOCIAL","PSICOLOGO"})mvc.perform(post("/api/vinculaciones/1/respuesta").header("X-User-Role",rol).header("X-User-Id","ACTOR").contentType("application/json").content("{\"respuesta\":\"ACEPTAR\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void propiasRechazaRolInstitucional() throws Exception {mvc.perform(get("/api/vinculaciones/propias").header("X-User-Role","COMITE").header("X-User-Id","CM-1")).andExpect(status().isForbidden());verifyNoInteractions(service);}
}
