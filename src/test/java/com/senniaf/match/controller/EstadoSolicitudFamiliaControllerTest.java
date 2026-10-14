package com.senniaf.match.controller;

import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.service.*;
import com.senniaf.match.support.ServiciosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EstadoSolicitudFamiliaControllerTest {
    private MockMvc mvc;

    @BeforeEach void preparar() {
        PerfilFamiliaRepository familias = mock(PerfilFamiliaRepository.class);
        DecisionComiteRepository decisiones = mock(DecisionComiteRepository.class);
        PerfilFamilia perfilA = new PerfilFamilia();
        perfilA.setCodigoCaso("FAM-A");
        perfilA.setRegistradoPor("8-111-111");
        perfilA.setPerfilCompleto(true);
        PerfilFamilia perfilB = new PerfilFamilia();
        perfilB.setCodigoCaso("FAM-B");
        perfilB.setRegistradoPor("8-222-222");
        perfilB.setPerfilCompleto(true);
        when(familias.findByRegistradoPor("8-111-111")).thenReturn(Optional.of(perfilA));
        when(familias.findByRegistradoPor("8-222-222")).thenReturn(Optional.of(perfilB));

        DecisionComite aceptacion = new DecisionComite();
        aceptacion.setCodigoNino("NNA-PRIVADO");
        aceptacion.setCodigoFamilia("FAM-A");
        aceptacion.setDecision("ACEPTADA");
        aceptacion.setMotivo("Motivo exclusivamente institucional");
        aceptacion.setFecha(LocalDateTime.of(2026,10,14,12,30));
        when(decisiones.findFirstByCodigoFamiliaAndDecisionOrderByFechaDescIdDesc("FAM-A","ACEPTADA"))
                .thenReturn(Optional.of(aceptacion));

        Usuario cuentaA = new Usuario(); cuentaA.setRol("FAMILIA"); cuentaA.setIdentificador("8-111-111");
        Usuario cuentaB = new Usuario(); cuentaB.setRol("FAMILIA"); cuentaB.setIdentificador("8-222-222");
        Usuario ts = new Usuario(); ts.setRol("TRABAJADOR_SOCIAL"); ts.setIdentificador("TS-001");
        AutenticacionService auth = ServiciosDePrueba.autenticacion(Map.of(
                "Bearer familiaA", cuentaA, "Bearer familiaB", cuentaB, "Bearer ts", ts));
        mvc = MockMvcBuilders.standaloneSetup(new PerfilFamiliaController(
                new PerfilFamiliaService(familias, decisiones), auth))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void laCuentaAceptadaVeUnicamenteSuEstado() throws Exception {
        mvc.perform(get("/api/familias/mi-perfil").header("Authorization","Bearer familiaA"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.codigoCaso").value("FAM-A"))
                .andExpect(jsonPath("$.estadoSolicitud").value("ACEPTADA"))
                .andExpect(jsonPath("$.fechaDecisionComite").exists())
                .andExpect(jsonPath("$.codigoNino").doesNotExist())
                .andExpect(jsonPath("$.motivo").doesNotExist());
    }

    @Test void laOtraFamiliaNoVeLaAceptacionAjena() throws Exception {
        mvc.perform(get("/api/familias/mi-perfil").header("Authorization","Bearer familiaB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoCaso").value("FAM-B"))
                .andExpect(jsonPath("$.estadoSolicitud").value("EN_ESPERA"));
    }

    @Test void sinSesionYTrabajadorSocialNoAccedenAlEstado() throws Exception {
        mvc.perform(get("/api/familias/mi-perfil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/familias/mi-perfil").header("Authorization","Bearer ts"))
                .andExpect(status().isForbidden());
    }
}
