package com.senniaf.match.service;

import com.senniaf.match.model.DecisionComite;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.repository.DecisionComiteRepository;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstadoSolicitudFamiliaServiceTest {
    private PerfilFamiliaRepository familias;
    private DecisionComiteRepository decisiones;
    private PerfilFamiliaService servicio;
    private final String cedula = "8-123-456";
    private final String codigo = "FAM-TEST";

    @BeforeEach void preparar() {
        familias = mock(PerfilFamiliaRepository.class);
        decisiones = mock(DecisionComiteRepository.class);
        servicio = new PerfilFamiliaService(familias, decisiones);
        PerfilFamilia perfil = new PerfilFamilia();
        perfil.setCodigoCaso(codigo);
        perfil.setRegistradoPor(cedula);
        perfil.setPerfilCompleto(true);
        when(familias.findByRegistradoPor(cedula)).thenReturn(Optional.of(perfil));
    }

    @Test void familiaAceptadaVeFechaYEstadoSinDatosDelNino() {
        DecisionComite aceptacion = new DecisionComite();
        aceptacion.setCodigoFamilia(codigo);
        aceptacion.setCodigoNino("NNA-PRIVADO");
        aceptacion.setDecision("ACEPTADA");
        aceptacion.setMotivo("Información clínica confidencial que no debe mostrarse");
        aceptacion.setFecha(LocalDateTime.of(2026,10,14,12,30));
        when(decisiones.findFirstByCodigoFamiliaAndDecisionOrderByFechaDescIdDesc(codigo, "ACEPTADA"))
                .thenReturn(Optional.of(aceptacion));

        var estado = servicio.consultarPorCedula(cedula).orElseThrow();
        assertEquals("ACEPTADA", estado.getEstadoSolicitud());
        assertEquals(aceptacion.getFecha(), estado.getFechaDecisionComite());
        assertEquals(codigo, estado.getCodigoCaso());
        assertFalse(estado.getMensaje().contains("NNA-PRIVADO"));
        assertFalse(estado.getMensaje().contains("clínica"));
    }

    @Test void entrevistaCompletaSinDecisionSeMantieneEnEspera() {
        var estado = servicio.consultarPorCedula(cedula).orElseThrow();
        assertEquals("EN_ESPERA", estado.getEstadoSolicitud());
        assertNull(estado.getFechaDecisionComite());
    }

    @Test void reevaluacionEsRevisionNoAceptacion() {
        DecisionComite reevaluar = new DecisionComite();
        reevaluar.setDecision("REEVALUAR");
        when(decisiones.findFirstByCodigoFamiliaOrderByFechaDescIdDesc(codigo)).thenReturn(Optional.of(reevaluar));
        assertEquals("EN_REVISION", servicio.consultarPorCedula(cedula).orElseThrow().getEstadoSolicitud());
    }

    @Test void otraCedulaNoPuedeConsultarElEstadoAjeno() {
        assertTrue(servicio.consultarPorCedula("8-999-999").isEmpty());
        verify(decisiones, never()).findFirstByCodigoFamiliaAndDecisionOrderByFechaDescIdDesc(anyString(), anyString());
    }
}
