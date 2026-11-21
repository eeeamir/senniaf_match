package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.senniaf.match.model.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
@Import(CitasPostgresIT.Config.class)
@TestPropertySource(properties={"senniaf.citas.franjas=1@08:00-16:00,2@08:00-16:00,3@08:00-16:00,4@08:00-16:00,5@08:00-16:00","senniaf.citas.intervalo-minutos=30"})
class CitasPostgresIT extends SoportePostgresIT {
    public static class Reloj extends Clock {
        final AtomicReference<Instant> instante=new AtomicReference<>(Instant.parse("2026-11-23T12:00:00Z"));
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zona){return Clock.fixed(instant(),zona);}
        public Instant instant(){return instante.get();}
    }
    @TestConfiguration static class Config {@Bean @Primary Reloj relojCitas(){return new Reloj();}}
    @Autowired Reloj relojCitas;
    String ts,tokenTs;
    record Contexto(Caso caso,long vin){}
    @BeforeEach void prepararAgenda(){
        relojCitas.instante.set(Instant.parse("2026-11-23T12:00:00Z"));
        ts="CIT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);usuario(ts,"TRABAJADOR_SOCIAL");tokenTs=login(ts);
    }
    Contexto vinculada(){
        Caso c=caso();long id=asignar(c);
        assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        c.n().setTrabajadorSocialAsignado(ts);ninos.saveAndFlush(c.n());return new Contexto(c,id);
    }
    ResponseEntity<String> crear(Contexto c,String fecha,String hora){return req("/api/citas",HttpMethod.POST,c.caso.token(),Map.of("vinculacionId",c.vin,"motivo","ORIENTACION","fecha",fecha,"hora",hora));}
    JsonNode solicitar(Contexto c){var r=crear(c,"2026-11-24","10:00");assertEquals(200,r.getStatusCode().value(),r.getBody());return datos(r);}
    ResponseEntity<String> actuar(JsonNode c,String token,String accion,Map<String,Object> valores){
        var body=new HashMap<String,Object>(valores);body.put("accion",accion);body.put("version",c.path("version").asLong());
        return req("/api/citas/"+c.path("id").asLong()+"/acciones",HttpMethod.POST,token,body);
    }
    JsonNode aceptar(JsonNode c){var r=actuar(c,tokenTs,"ACEPTAR",Map.of());assertEquals(200,r.getStatusCode().value(),r.getBody());return datos(r);}
    @Test void recorridoPersistidoDeSolicitudYAceptacionSinJustificacion(){
        var ctx=vinculada();var c=solicitar(ctx);assertEquals("PENDIENTE",c.path("estado").asText());assertEquals(120,c.path("duracion").asInt());
        var confirmada=aceptar(c);assertEquals("CONFIRMADA",confirmada.path("estado").asText());
        assertEquals("2026-11-24T12:00:00-05:00",confirmada.path("fin").asText());assertEquals(2,confirmada.path("historial").size());
        assertTrue(confirmada.path("mensaje").asText().contains("10:00 a. m."));
        assertEquals(1,jdbc.queryForObject("select count(*) from citas_vinculacion where id=? and estado='CONFIRMADA'",Integer.class,c.path("id").asLong()));
        assertFalse(req("/api/citas",HttpMethod.GET,ctx.caso.token(),null).getBody().contains(ctx.caso.n().getCodigoCaso()));
    }
    @Test void rechazarMantieneMotivoMensajeYEstadoTerminal(){
        var ctx=vinculada();var c=solicitar(ctx);var r=actuar(c,tokenTs,"RECHAZAR",Map.of("motivo","COORDINACION_PREVIA"));
        assertEquals(200,r.getStatusCode().value(),r.getBody());assertEquals("RECHAZADA",datos(r).path("estado").asText());
        assertTrue(datos(r).path("mensaje").asText().contains("coordinación previa"));assertEquals(409,actuar(datos(r),tokenTs,"ACEPTAR",Map.of()).getStatusCode().value());
        assertEquals(2,jdbc.queryForObject("select count(*) from citas_historial where cita_id=?",Integer.class,c.path("id").asLong()));
    }
    @Test void propuestaYContrapropuestaNoCreanCitasAdicionales(){
        var ctx=vinculada();var c=solicitar(ctx);var p=actuar(c,tokenTs,"PROPONER",Map.of("fecha","2026-11-25","hora","13:00","duracion",60,"motivo","AJUSTE_AGENDA"));
        assertEquals(200,p.getStatusCode().value(),p.getBody());var nueva=datos(p);assertEquals("CAMBIO_PROPUESTO",nueva.path("estado").asText());
        assertTrue(nueva.path("mensaje").asText().contains("1:00 p. m."));assertFalse(nueva.path("mensaje").asText().contains("[fecha]"));
        var contra=actuar(nueva,ctx.caso.token(),"OTRO_HORARIO",Map.of("fecha","2026-11-25","hora","15:00"));
        assertEquals(200,contra.getStatusCode().value(),contra.getBody());assertEquals("PENDIENTE",datos(contra).path("estado").asText());
        var confirmada=actuar(datos(contra),tokenTs,"ACEPTAR",Map.of("duracion",60));assertEquals(200,confirmada.getStatusCode().value(),confirmada.getBody());
        assertEquals(4,datos(confirmada).path("historial").size());assertEquals(1,jdbc.queryForObject("select count(*) from citas_vinculacion where vinculacion_id=?",Integer.class,ctx.vin));
        assertEquals("2026-11-24 10:00:00",jdbc.queryForObject("select inicio::text from citas_historial where cita_id=? order by id limit 1",String.class,c.path("id").asLong()));
    }
    @Test void familiaPuedeAceptarNuevaFechaConDisponibilidadVigente(){
        var ctx=vinculada();var c=solicitar(ctx);var r=actuar(c,tokenTs,"PROPONER",Map.of("fecha","2026-11-25","hora","14:00","duracion",120,"motivo","NUEVO_HORARIO"));
        assertEquals(200,r.getStatusCode().value(),r.getBody());var aceptada=actuar(datos(r),ctx.caso.token(),"ACEPTAR_PROPUESTA",Map.of());
        assertEquals(200,aceptada.getStatusCode().value(),aceptada.getBody());assertEquals("CONFIRMADA",datos(aceptada).path("estado").asText());
        assertEquals(409,actuar(datos(aceptada),ctx.caso.token(),"ACEPTAR_PROPUESTA",Map.of()).getStatusCode().value());
    }
    @Test void aislamientoPorFamiliaYProfesionalYCabecerasNoSuplantan(){
        var ctx=vinculada();var ajena=vinculada();var c=solicitar(ctx);long id=c.path("id").asLong();
        for(String t:List.of(ajena.caso.token(),otro,psicologo,comite)) {
            assertEquals(403,req("/api/citas/"+id,HttpMethod.GET,t,null).getStatusCode().value());
            assertEquals(403,actuar(c,t,"ACEPTAR",Map.of()).getStatusCode().value());
        }
        assertEquals(403,req("/api/citas",HttpMethod.POST,ajena.caso.token(),Map.of("vinculacionId",ctx.vin,"motivo","DUDAS","fecha","2026-11-24","hora","10:00")).getStatusCode().value());
        assertEquals(401,req("/api/citas",HttpMethod.GET,null,null).getStatusCode().value());
        assertEquals("[]",req("/api/citas",HttpMethod.GET,otro,null).getBody());
        HttpHeaders h=new HttpHeaders();h.setBearerAuth(ajena.caso.token());h.set("X-User-Role","TRABAJADOR_SOCIAL");h.set("X-User-Id",ts);
        assertEquals(403,http.exchange("/api/citas/"+id,HttpMethod.GET,new HttpEntity<>(h),String.class).getStatusCode().value());
    }
    @Test void fechasManipuladasYDuracionesIncompatiblesSeRechazan(){
        var ctx=vinculada();for(String[] v:List.of(new String[]{"2026-02-30","10:00"},new String[]{"2026-11-22","10:00"},new String[]{"2026-11-23","06:30"},new String[]{"2026-11-28","10:00"},new String[]{"2026-11-24","25:00"},new String[]{"2026-11-24","14:30"},new String[]{"2026-11-24","09:15"}))
            assertEquals(400,crear(ctx,v[0],v[1]).getStatusCode().value());
        var c=solicitar(ctx);assertEquals(400,actuar(c,tokenTs,"ACEPTAR",Map.of("duracion",90)).getStatusCode().value());
        assertEquals("PENDIENTE",datos(req("/api/citas/"+c.path("id").asLong(),HttpMethod.GET,ctx.caso.token(),null)).path("estado").asText());
    }
    @Test void disponibilidadEsCompartidaYVerificaFinalizacion(){
        var ctx=vinculada();String ruta="/api/citas/disponibilidad?vinculacionId="+ctx.vin+"&fecha=2026-11-24&duracion=120";
        var horas=req(ruta,HttpMethod.GET,ctx.caso.token(),null);assertEquals(200,horas.getStatusCode().value());assertTrue(horas.getBody().contains("14:00"));assertFalse(horas.getBody().contains("14:30"));
        assertEquals(horas.getBody(),req(ruta,HttpMethod.GET,tokenTs,null).getBody());
        assertTrue(req(ruta.replace("120","60"),HttpMethod.GET,tokenTs,null).getBody().contains("15:00"));
        assertEquals("[]",req(ruta.replace("2026-11-24","2026-11-28"),HttpMethod.GET,tokenTs,null).getBody());
    }
    @Test void noVinculadaSinAsignacionCierreEInterrupcionBloquean(){
        var c=caso();long vin=asignar(c);var ctx=new Contexto(c,vin);assertEquals(409,crear(ctx,"2026-11-24","10:00").getStatusCode().value());
        assertEquals(200,req("/api/vinculaciones/"+vin+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        c.n().setTrabajadorSocialAsignado(null);ninos.saveAndFlush(c.n());assertEquals(409,crear(ctx,"2026-11-24","10:00").getStatusCode().value());
        c.n().setTrabajadorSocialAsignado(ts);c.n().setBajaLogica(true);ninos.saveAndFlush(c.n());assertEquals(409,crear(ctx,"2026-11-24","10:00").getStatusCode().value());
    }
    @Test void solicitudesSimultaneasDeMismaVinculacionNoDuplican()throws Exception {
        var ctx=vinculada();assertEquals(List.of(200,409),simultaneas(()->crear(ctx,"2026-11-24","10:00"),()->crear(ctx,"2026-11-24","10:00")));
        assertEquals(1,jdbc.queryForObject("select count(*) from citas_vinculacion where vinculacion_id=?",Integer.class,ctx.vin));
        assertEquals(1,jdbc.queryForObject("select count(*) from citas_historial h join citas_vinculacion c on c.id=h.cita_id where c.vinculacion_id=?",Integer.class,ctx.vin));
    }
    @Test void soloUnaReservaSuperpuestaSeConfirmaEnConcurrencia()throws Exception {
        var a=vinculada();var b=vinculada();var ca=solicitar(a);var rb=crear(b,"2026-11-24","11:00");assertEquals(200,rb.getStatusCode().value(),rb.getBody());var cb=datos(rb);
        assertEquals(List.of(200,409),simultaneas(()->actuar(ca,tokenTs,"ACEPTAR",Map.of()),()->actuar(cb,tokenTs,"ACEPTAR",Map.of())));
        assertEquals(1,jdbc.queryForObject("select count(*) from citas_vinculacion where profesional=? and estado='CONFIRMADA'",Integer.class,ts));
    }
    @Test void respuestaSimultaneaSobreMismaVersionNoSobrescribe()throws Exception {
        var ctx=vinculada();var c=solicitar(ctx);
        assertEquals(List.of(200,409),simultaneas(()->actuar(c,tokenTs,"ACEPTAR",Map.of()),()->actuar(c,tokenTs,"RECHAZAR",Map.of("motivo","SIN_DISPONIBILIDAD"))));
        assertEquals(2,jdbc.queryForObject("select count(*) from citas_historial where cita_id=?",Integer.class,c.path("id").asLong()));
    }
    @Test void propuestaFamiliarSeRevalidaContraOtraConfirmacion(){
        var a=vinculada();var b=vinculada();var ca=solicitar(a);
        var pro=actuar(ca,tokenTs,"PROPONER",Map.of("fecha","2026-11-25","hora","10:00","motivo","NUEVO_HORARIO"));assertEquals(200,pro.getStatusCode().value(),pro.getBody());
        var rb=crear(b,"2026-11-25","10:30");assertEquals(200,rb.getStatusCode().value(),rb.getBody());aceptar(datos(rb));
        assertEquals(409,actuar(datos(pro),a.caso.token(),"ACEPTAR_PROPUESTA",Map.of()).getStatusCode().value());
        assertEquals("CAMBIO_PROPUESTO",datos(req("/api/citas/"+ca.path("id").asLong(),HttpMethod.GET,a.caso.token(),null)).path("estado").asText());
    }
    @Test void intervalosAdyacentesPermitidosYCancelacionLiberaAgenda(){
        var a=vinculada();var b=vinculada();var ca=aceptar(solicitar(a));var rb=crear(b,"2026-11-24","12:00");assertEquals(200,rb.getStatusCode().value(),rb.getBody());aceptar(datos(rb));
        assertEquals(200,actuar(ca,a.caso.token(),"CANCELAR",Map.of()).getStatusCode().value());
        var nuevo=crear(a,"2026-11-24","10:00");assertEquals(200,nuevo.getStatusCode().value(),nuevo.getBody());aceptar(datos(nuevo));
    }
    @Test void realizadaSoloProfesionalDespuesDelFinal(){
        var ctx=vinculada();var c=aceptar(solicitar(ctx));
        assertEquals(409,actuar(c,tokenTs,"REALIZADA",Map.of()).getStatusCode().value());
        assertEquals(403,actuar(c,ctx.caso.token(),"REALIZADA",Map.of()).getStatusCode().value());
        relojCitas.instante.set(Instant.parse("2026-11-24T17:00:00Z"));
        assertEquals(409,actuar(c,ctx.caso.token(),"CANCELAR",Map.of()).getStatusCode().value());
        var r=actuar(c,tokenTs,"REALIZADA",Map.of());assertEquals(200,r.getStatusCode().value(),r.getBody());assertEquals("REALIZADA",datos(r).path("estado").asText());
    }
    @Test void historialNoAdmiteAlteracionNiSeFiltraATerceros(){
        var ctx=vinculada();var c=solicitar(ctx);assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update citas_historial set mensaje='cambio no permitido' where cita_id=?",c.path("id").asLong()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from citas_historial where cita_id=?",c.path("id").asLong()));
        assertEquals(1,jdbc.queryForObject("select count(*) from citas_historial where cita_id=?",Integer.class,c.path("id").asLong()));
    }
    @Test void reasignacionRealNoPermiteResponderAlProfesionalAnterior(){
        var ctx=vinculada();var c=solicitar(ctx);String nuevo="CIT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        usuario(nuevo,"TRABAJADOR_SOCIAL");String token=login(nuevo);
        ctx.caso.n().setTrabajadorSocialAsignado(nuevo);ninos.saveAndFlush(ctx.caso.n());
        assertEquals(403,actuar(c,tokenTs,"ACEPTAR",Map.of()).getStatusCode().value());
        var r=actuar(c,token,"ACEPTAR",Map.of());assertEquals(200,r.getStatusCode().value(),r.getBody());
        assertEquals(List.of(ts,nuevo),jdbc.queryForList("select profesional from citas_historial where cita_id=? order by id",String.class,c.path("id").asLong()));
    }
    @Test void rechazoNoPuedeEludirseMedianteCancelacionProfesionalDePendiente(){
        var ctx=vinculada();var c=solicitar(ctx);assertFalse(c.path("acciones").toString().contains("ACEPTAR"));
        assertEquals(409,actuar(c,tokenTs,"CANCELAR",Map.of()).getStatusCode().value());
        var r=req("/api/citas/"+c.path("id").asLong(),HttpMethod.GET,tokenTs,null);
        assertFalse(datos(r).path("acciones").toString().contains("CANCELAR"));
    }
    @Test void parametrosManipuladosNoProducenErrorInterno(){
        var ctx=vinculada();
        for(String ruta:List.of("/api/citas/no-es-numero","/api/citas/disponibilidad?vinculacionId=x&fecha=2026-11-24","/api/citas/disponibilidad?vinculacionId="+ctx.vin+"&fecha=2026-11-24&duracion=abc","/api/citas/disponibilidad?vinculacionId="+ctx.vin))
            assertEquals(400,req(ruta,HttpMethod.GET,ctx.caso.token(),null).getStatusCode().value());
    }
}
