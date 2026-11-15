package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VinculacionServiceTest {
    VinculacionRepository repo;
    EventoCasoRepository auditoria;
    VinculacionService service;
    Vinculacion v;
    @BeforeEach void preparar(){
        repo=mock(VinculacionRepository.class);auditoria=mock(EventoCasoRepository.class);service=new VinculacionService(repo,auditoria);
        var n=new PerfilNino();n.setCodigoCaso("NNA-001");n.setTrabajadorSocialAsignado("TS-1");n.setPsicologoAsignado("PS-1");
        var f=new PerfilFamilia();f.setCodigoCaso("FAM-001");f.setRegistradoPor("FAMILIA-1");
        var d=new DecisionComite();d.setDecision("ACEPTADA");d.setFuncionario("CM-1");d.setFecha(LocalDateTime.now());d.setMotivo("Revisión favorable de expedientes");
        v=new Vinculacion();ReflectionTestUtils.setField(v,"id",1L);v.setNino(n);v.setFamilia(f);v.setDecision(d);
        when(repo.bloquear(1L)).thenReturn(Optional.of(v));when(repo.findById(1L)).thenReturn(Optional.of(v));when(repo.findAllByOrderByIdDesc()).thenReturn(List.of(v));
        when(repo.findByFamiliaRegistradoPorOrderByIdDesc("FAMILIA-1")).thenReturn(List.of(v));
    }
    @Test void aceptarMantieneReservaYAudita(){
        var resultado=service.responder(1L,"FAMILIA-1","ACEPTAR");
        assertEquals(Vinculacion.Estado.RESPUESTA_ACEPTADA,resultado.get("estado"));assertTrue(v.isReservaVigente());assertNotNull(v.getFechaRespuesta());assertEquals("FAMILIA-1",v.getRespondidoPor());
        verify(auditoria).save(argThat(e->e.getDescripcion().contains("RESPUESTA_ACEPTADA")&&e.getActor().equals("FAMILIA-1")));
    }
    @Test void rechazarLiberaPropuestaSinCambiarIdoneidad(){
        service.responder(1L,"FAMILIA-1","RECHAZAR");assertEquals(Vinculacion.Estado.RECHAZADA_POR_FAMILIA,v.getEstado());assertFalse(v.isReservaVigente());assertFalse(v.getNino().isBajaLogica());assertFalse(v.getNino().isCierrePorAdopcion());assertFalse(v.getFamilia().isReevaluacionPendiente());
        assertFalse(service.propias("FAMILIA-1").get(0).containsKey("expedienteInfantil"));
    }
    @Test void respuestaNoSePuedeCambiar(){service.responder(1L,"FAMILIA-1","RECHAZAR");assertThrows(IllegalStateException.class,()->service.responder(1L,"FAMILIA-1","ACEPTAR"));verify(repo,times(1)).save(v);}
    @Test void otraFamiliaNoPuedeResponder(){assertThrows(RolNoAutorizadoException.class,()->service.responder(1L,"OTRA","ACEPTAR"));verify(repo,never()).save(any());verifyNoInteractions(auditoria);}
    @Test void valorManipuladoNoSeGuarda(){assertThrows(IllegalArgumentException.class,()->service.responder(1L,"FAMILIA-1","CERRADA"));verify(repo,never()).bloquear(any());}
    @Test void familiaNoVeDetalleInstitucional(){assertThrows(RolNoAutorizadoException.class,()->service.detalle(1L,"FAMILIA","FAMILIA-1"));}
    @Test void profesionalesSoloVenAsignacionesPropias(){assertEquals(1,service.listar("PSICOLOGO","PS-1",null,null).size());assertTrue(service.listar("PSICOLOGO","OTRO",null,null).isEmpty());assertThrows(RolNoAutorizadoException.class,()->service.detalle(1L,"TRABAJADOR_SOCIAL","OTRO"));}
    @Test void filtrosNoAmplianAcceso(){assertTrue(service.listar("PSICOLOGO","OTRO",null,"NNA-001").isEmpty());assertEquals(1,service.listar("COMITE","CM-1","PENDIENTE_RESPUESTA","fam-001").size());assertTrue(service.listar("COMITE","CM-1","RESPUESTA_ACEPTADA",null).isEmpty());}
    @Test void portalNoExponeDatosInfantilesNiDecisionInterna(){var r=service.propias("FAMILIA-1").get(0);assertFalse(r.containsKey("nino"));assertFalse(r.containsKey("expedienteInfantil"));assertFalse(r.containsKey("decisionComite"));assertFalse(r.containsKey("historial"));assertTrue(service.propias("OTRA").isEmpty());}
    @Test void noCreaSobreReservaExistente(){when(repo.existsByNinoCodigoCasoAndReservaVigenteTrue("NNA-001")).thenReturn(true);assertThrows(IllegalStateException.class,()->service.crear(v.getDecision(),v.getNino(),v.getFamilia()));verify(repo,never()).saveAndFlush(any());}
    @Test void crearConservaExpedientesYAuditoria(){when(repo.saveAndFlush(any())).thenAnswer(a->{Vinculacion nueva=a.getArgument(0);ReflectionTestUtils.setField(nueva,"id",2L);return nueva;});service.crear(v.getDecision(),v.getNino(),v.getFamilia());verify(repo).saveAndFlush(argThat(n->n.getNino()==v.getNino()&&n.getFamilia()==v.getFamilia()&&n.getEstado()==Vinculacion.Estado.PENDIENTE_RESPUESTA));verify(auditoria).save(argThat(e->e.getAccion().equals("VINCULACION_CREADA")));}
    @Test void historialSoloIncluyeEstaVinculacion(){var e=new EventoCaso();e.setAccion("RESPUESTA_FAMILIAR");e.setActor("FAMILIA-1");e.setFecha(LocalDateTime.now());e.setDescripcion("{\"vinculacionId\":10,\"resumen\":\"Otro proceso\"}");when(auditoria.findByCodigoCasoOrderByFechaAsc("NNA-001")).thenReturn(List.of(e));assertEquals(List.of(),service.detalle(1L,"COMITE","CM-1").get("historial"));}
    @Test void historialAdmiteJsonConEspaciosYSinCamposOpcionales(){
        var e=new EventoCaso();e.setAccion("RESPUESTA_FAMILIAR");e.setActor("FAMILIA-1");
        e.setDescripcion("{ \"resumen\": \"Respuesta registrada\", \"vinculacionId\": 1 }");
        var vacio=new EventoCaso();vacio.setDescripcion("");
        when(auditoria.findByCodigoCasoOrderByFechaAsc("NNA-001")).thenReturn(List.of(vacio,e));
        var historial=(List<?>)service.detalle(1L,"COMITE","CM-1").get("historial");
        assertEquals(1,historial.size());assertEquals("Respuesta registrada",((Map<?,?>)historial.get(0)).get("resumen"));
    }

}
