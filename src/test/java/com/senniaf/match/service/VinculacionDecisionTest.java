package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VinculacionDecisionTest {
    PerfilNinoRepository ninos=mock(PerfilNinoRepository.class);
    PerfilFamiliaRepository familias=mock(PerfilFamiliaRepository.class);
    DecisionComiteRepository decisiones=mock(DecisionComiteRepository.class);
    PesosScoringRepository pesos=mock(PesosScoringRepository.class);
    VinculacionRepository reservas=mock(VinculacionRepository.class);
    VinculacionService vinculaciones=mock(VinculacionService.class);
    ScoringService service=new ScoringService(ninos,familias,decisiones,pesos);
    PerfilNino n=new PerfilNino();PerfilFamilia f=new PerfilFamilia();
    @BeforeEach void preparar(){
        n.setCodigoCaso("NNA-001");n.setPerfilCompleto(true);f.setCodigoCaso("FAM-001");f.setPerfilCompleto(true);
        when(ninos.bloquear("NNA-001")).thenReturn(Optional.of(n));when(familias.bloquear("FAM-001")).thenReturn(Optional.of(f));
        when(pesos.findById(1L)).thenReturn(Optional.of(new PesosScoring()));
        ReflectionTestUtils.setField(service,"vinculaciones",vinculaciones);ReflectionTestUtils.setField(service,"vinculacionesRepo",reservas);
    }
    @Test void aceptarCreaVinculacionConMismosExpedientes(){service.decidir("NNA-001","FAM-001","ACEPTADA","Revisión favorable del Comité","CM-1");verify(vinculaciones).crear(argThat(d->d.getDecision().equals("ACEPTADA")&&d.getFuncionario().equals("CM-1")),same(n),same(f));verify(ninos,never()).save(any());verify(familias,never()).save(any());}
    @Test void rechazarPropuestaNoCreaVinculacion(){service.decidir("NNA-001","FAM-001","DESCARTADA","Revisión desfavorable de propuesta","CM-1");verifyNoInteractions(vinculaciones);verify(decisiones).save(argThat(d->d.getDecision().equals("DESCARTADA")));}
    @Test void reservaFamiliarBloqueaOtraAceptacion(){when(reservas.existsByFamiliaCodigoCasoAndReservaVigenteTrue("FAM-001")).thenReturn(true);assertThrows(IllegalStateException.class,()->service.decidir("NNA-001","FAM-001","ACEPTADA","Revisión favorable del Comité","CM-1"));verifyNoInteractions(vinculaciones);verify(decisiones,never()).save(any());}
    @Test void reservaInfantilBloqueaOtraAceptacion(){when(reservas.existsByNinoCodigoCasoAndReservaVigenteTrue("NNA-001")).thenReturn(true);assertThrows(IllegalStateException.class,()->service.decidir("NNA-001","FAM-001","ACEPTADA","Revisión favorable del Comité","CM-1"));verifyNoInteractions(vinculaciones);}
    @Test void propuestaYaNoElegibleNoSeAcepta(){n.setBajaLogica(true);assertThrows(IllegalStateException.class,()->service.decidir("NNA-001","FAM-001","ACEPTADA","Revisión favorable del Comité","CM-1"));verifyNoInteractions(vinculaciones);}
    @Test void conservaSolicitudReevaluacion(){service.decidir("NNA-001","FAM-001","REEVALUACION","Completar información de entrevista","CM-1");assertTrue(f.isReevaluacionPendiente());verify(familias).save(f);verifyNoInteractions(vinculaciones);}
}
