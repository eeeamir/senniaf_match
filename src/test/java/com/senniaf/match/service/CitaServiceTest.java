package com.senniaf.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.model.CitaVinculacion.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.security.RolNoAutorizadoException;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CitaServiceTest {
    final CitaVinculacionRepository citas=mock(CitaVinculacionRepository.class);
    final CitaHistorialRepository historial=mock(CitaHistorialRepository.class);
    final VinculacionRepository vinculaciones=mock(VinculacionRepository.class);
    final InterrupcionConvivenciaRepository interrupciones=mock(InterrupcionConvivenciaRepository.class);
    final UsuarioRepository usuarios=mock(UsuarioRepository.class);
    final EventoCasoRepository auditoria=mock(EventoCasoRepository.class);
    final Clock reloj=Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"),ZoneOffset.UTC);
    final List<CitaHistorial> eventos=new ArrayList<>();
    final Map<Long,CitaVinculacion> guardadas=new HashMap<>();
    final Vinculacion v=new Vinculacion();
    CitaService servicio;
    @BeforeEach void preparar(){
        var n=PerfilesCompatibilidad.nino();n.setTrabajadorSocialAsignado("TS-1");
        ReflectionTestUtils.setField(n,"id",1L);
        var f=PerfilesCompatibilidad.familia();f.setRegistradoPor("FAM-1");
        ReflectionTestUtils.setField(v,"id",3L);v.setNino(n);v.setFamilia(f);v.setEstado(Vinculacion.Estado.RESPUESTA_ACEPTADA);
        var usuario=new Usuario();usuario.setIdentificador("TS-1");usuario.setRol("TRABAJADOR_SOCIAL");usuario.setVerificado(true);
        when(vinculaciones.findById(3L)).thenReturn(Optional.of(v));when(vinculaciones.bloquear(3L)).thenReturn(Optional.of(v));
        when(vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc("FAM-1")).thenReturn(List.of(v));
        when(citas.bloquearAgenda("TS-1")).thenReturn(Optional.of(usuario));when(usuarios.findByIdentificador("TS-1")).thenReturn(Optional.of(usuario));
        when(citas.findById(anyLong())).thenAnswer(a->Optional.ofNullable(guardadas.get(a.getArgument(0))));
        when(citas.bloquearSolicitud(anyLong())).thenAnswer(a->Optional.of(a.getArgument(0)));
        when(citas.bloquearVinculo(3L)).thenReturn(Optional.of(3L));
        when(citas.bloquearExpediente(1L)).thenReturn(Optional.of(1L));
        when(citas.saveAndFlush(any())).thenAnswer(a->{CitaVinculacion c=a.getArgument(0);if(c.getId()==null)ReflectionTestUtils.setField(c,"id",(long)guardadas.size()+1);
            else ReflectionTestUtils.setField(c,"version",c.getVersion()+1);guardadas.put(c.getId(),c);return c;});
        when(historial.save(any())).thenAnswer(a->{CitaHistorial e=a.getArgument(0);eventos.add(e);return e;});
        when(historial.findByCitaIdOrderByIdAsc(anyLong())).thenAnswer(a->eventos.stream().filter(e->e.getCita().getId().equals(a.getArgument(0))).toList());
        servicio=new CitaService(citas,historial,vinculaciones,interrupciones,usuarios,auditoria,
                new HorarioCitas("1@08:00-16:00,2@08:00-16:00",30),reloj,new ObjectMapper(),mock(EntityManager.class));
    }
    Map<String,Object> solicitar(){return servicio.solicitar(new CitaService.Solicitud(3L,"ORIENTACION",null,"2026-11-24","10:00"),"FAMILIA","FAM-1");}
    Map<String,Object> responder(String accion,String rol,String actor,String fecha,String hora,Integer duracion,String motivo,String mensaje){
        var c=guardadas.get(1L);return servicio.responder(1L,new CitaService.Respuesta(accion,c.getVersion(),fecha,hora,duracion,motivo,mensaje),rol,actor);
    }
    @Test void solicitudCompletaConDuracionDosHorasYAuditoria(){
        var r=solicitar();assertEquals(Estado.PENDIENTE,r.get("estado"));assertEquals(120,r.get("duracion"));
        assertEquals("TS-1",guardadas.get(1L).getProfesional());assertEquals("FAM-1",guardadas.get(1L).getCreadoPor());
        assertEquals(1,eventos.size());assertEquals("FAM-1",eventos.get(0).getActor());verify(auditoria).save(any());
        assertFalse(r.containsKey("familia"));assertFalse(r.toString().contains(v.getNino().getCodigoCaso()));
    }
    @Test void aceptaSinJustificacionYPermiteUnaHora(){
        solicitar();var r=responder("ACEPTAR","TRABAJADOR_SOCIAL","TS-1",null,null,60,null,null);
        assertEquals(Estado.CONFIRMADA,r.get("estado"));assertEquals(60,r.get("duracion"));
        assertTrue(r.get("mensaje").toString().contains("10:00 a. m."));assertEquals(2,eventos.size());
        assertFalse(r.get("mensaje").toString().contains("m.."));
    }
    @Test void propuestaContrapropuestaYAceptacionConservanTodoElHistorial(){
        solicitar();responder("PROPONER","TRABAJADOR_SOCIAL","TS-1","2026-11-24","13:00",60,"NUEVO_HORARIO",null);
        responder("OTRO_HORARIO","FAMILIA","FAM-1","2026-11-24","14:00",null,null,null);
        var r=responder("ACEPTAR","TRABAJADOR_SOCIAL","TS-1",null,null,60,null,null);
        assertEquals(Estado.CONFIRMADA,r.get("estado"));assertEquals(4,eventos.size());
        assertEquals(LocalDateTime.parse("2026-11-24T10:00"),eventos.get(0).getInicio());
        assertEquals(LocalDateTime.parse("2026-11-24T13:00"),eventos.get(1).getInicio());
        assertEquals(LocalDateTime.parse("2026-11-24T14:00"),eventos.get(2).getInicio());
    }
    @Test void familiaAceptaPropuestaSinCrearOtraCita(){
        solicitar();responder("PROPONER","TRABAJADOR_SOCIAL","TS-1","2026-11-24","13:00",120,"AJUSTE_AGENDA",null);
        assertEquals(Estado.CONFIRMADA,responder("ACEPTAR_PROPUESTA","FAMILIA","FAM-1",null,null,null,null,null).get("estado"));
        assertEquals(1,guardadas.size());assertEquals(3,eventos.size());
    }
    @Test void rechazoTieneMotivoYNoPierdeLaSolicitud(){
        solicitar();var r=responder("RECHAZAR","TRABAJADOR_SOCIAL","TS-1",null,null,null,"COORDINACION_PREVIA",null);
        assertEquals(Estado.RECHAZADA,r.get("estado"));assertTrue(r.get("mensaje").toString().contains("coordinación previa"));
        assertEquals(2,eventos.size());assertThrows(IllegalStateException.class,()->responder("ACEPTAR","TRABAJADOR_SOCIAL","TS-1",null,null,null,null,null));
    }
    @ParameterizedTest @ValueSource(strings={"COMITE","PSICOLOGO","ANONIMO",""})
    void otrosRolesNoAcceden(String rol){assertThrows(RolNoAutorizadoException.class,()->servicio.listar(rol,"TS-1"));}
    @Test void impideSuplantarFamiliaOProfesional(){
        assertThrows(RolNoAutorizadoException.class,()->servicio.solicitar(new CitaService.Solicitud(3L,"DUDAS",null,"2026-11-24","10:00"),"FAMILIA","FAM-2"));
        solicitar();assertThrows(RolNoAutorizadoException.class,()->servicio.detalle(1L,"FAMILIA","FAM-2"));
        assertThrows(RolNoAutorizadoException.class,()->responder("ACEPTAR","TRABAJADOR_SOCIAL","TS-2",null,null,null,null,null));
        assertThrows(RolNoAutorizadoException.class,()->responder("ACEPTAR","FAMILIA","FAM-1",null,null,null,null,null));
        assertThrows(RolNoAutorizadoException.class,()->responder("ACEPTAR_PROPUESTA","TRABAJADOR_SOCIAL","TS-1",null,null,null,null,null));
    }
    @ParameterizedTest @EnumSource(value=Vinculacion.Estado.class,names={"PENDIENTE_RESPUESTA","CERRADA","RECHAZADA_POR_FAMILIA","RESPUESTA_RECHAZADA_PENDIENTE_CIERRE"})
    void soloVinculacionAceptadaActiva(Vinculacion.Estado estado){v.setEstado(estado);assertThrows(IllegalStateException.class,this::solicitar);verify(citas,never()).saveAndFlush(any());}
    @Test void sinReservaOConCierreOInterrupcionNoSeSolicita(){
        v.setReservaVigente(false);assertThrows(IllegalStateException.class,this::solicitar);v.setReservaVigente(true);
        v.getNino().setCierrePorAdopcion(true);assertThrows(IllegalStateException.class,this::solicitar);v.getNino().setCierrePorAdopcion(false);
        when(interrupciones.findByVinculacionId(3L)).thenReturn(Optional.of(new InterrupcionConvivencia()));assertThrows(IllegalStateException.class,this::solicitar);
    }
    @Test void noInventaProfesionalSiFaltaAsignacion(){v.getNino().setTrabajadorSocialAsignado(null);assertThrows(IllegalStateException.class,this::solicitar);}
    @Test void bloqueaDuplicadoPendiente(){when(citas.existeAbierta(3L)).thenReturn(true);assertThrows(IllegalStateException.class,this::solicitar);verify(citas,never()).saveAndFlush(any());}
    @Test void revalidaSolapamientoAlAceptar(){
        solicitar();when(citas.hayConflicto(anyString(),any(),any(),any())).thenReturn(true);
        assertThrows(IllegalStateException.class,()->responder("ACEPTAR","TRABAJADOR_SOCIAL","TS-1",null,null,120,null,null));assertEquals(1,eventos.size());
    }
    @Test void rechazaRespuestaDeVersionAnterior(){
        solicitar();assertThrows(IllegalStateException.class,()->servicio.responder(1L,new CitaService.Respuesta("RECHAZAR",8L,null,null,null,"OTRO","Un motivo de prueba válido"),"TRABAJADOR_SOCIAL","TS-1"));
    }
    @ParameterizedTest @ValueSource(strings={"corto","Respuesta para [fecha] a las [hora]","Texto con carácter\u0000 oculto","Texto con carácter\u200b oculto"})
    void rechazaMensajesInvalidos(String mensaje){assertThrows(IllegalArgumentException.class,()->servicio.solicitar(new CitaService.Solicitud(3L,"DUDAS",mensaje,"2026-11-24","10:00"),"FAMILIA","FAM-1"));}
    @Test void cancelacionConservaHistorialYLaCitaNoSeReabre(){
        solicitar();assertEquals(Estado.CANCELADA,responder("CANCELAR","FAMILIA","FAM-1",null,null,null,null,null).get("estado"));
        assertEquals(2,eventos.size());assertThrows(IllegalStateException.class,()->responder("CANCELAR","FAMILIA","FAM-1",null,null,null,null,null));
    }
    @Test void noMarcaRealizadaAntesDeFinalizar(){
        solicitar();responder("ACEPTAR","TRABAJADOR_SOCIAL","TS-1",null,null,null,null,null);
        assertThrows(IllegalStateException.class,()->responder("REALIZADA","TRABAJADOR_SOCIAL","TS-1",null,null,null,null,null));
    }
    @Test void profesionalNoEludeElMotivoDeRechazoCancelandoUnaPendiente(){
        solicitar();assertThrows(IllegalStateException.class,()->responder("CANCELAR","TRABAJADOR_SOCIAL","TS-1",null,null,null,null,null));
        assertEquals(Estado.PENDIENTE,guardadas.get(1L).getEstado());
    }
}
