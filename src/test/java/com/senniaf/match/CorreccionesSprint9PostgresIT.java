package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.senniaf.match.model.CombinacionExcluida;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.repository.CombinacionExcluidaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class CorreccionesSprint9PostgresIT extends SoportePostgresIT {
    @Autowired CombinacionExcluidaRepository exclusiones;
    long interrumpida(Caso c){
        long id=asignar(c);
        assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        autorizarInicio(id);
        assertEquals(200,req("/api/vinculaciones/"+id+"/interrupcion",HttpMethod.POST,social,Map.of("motivo","ADAPTACION","justificacion","Interrupción sintética para comprobar la revisión independiente")).getStatusCode().value());
        assertEquals(200,req("/api/vinculaciones/"+id+"/cierre",HttpMethod.POST,comite,Map.of("confirmacionAutoridad",true,"motivo","Cierre institucional sintético con antecedentes conservados")).getStatusCode().value());
        return id;
    }
    JsonNode liberar(long id,String tipo){
        var r=req("/api/vinculaciones/"+id+"/disponibilidad",HttpMethod.POST,comite,Map.of("expediente",tipo,"justificacion","Se autoriza de manera independiente la disponibilidad y elegibilidad"));
        assertEquals(200,r.getStatusCode().value(),r.getBody());return datos(r);
    }
    JsonNode candidato(Caso c){
        var r=req("/api/scoring/casos/"+c.n().getCodigoCaso(),HttpMethod.GET,comite,null);assertEquals(200,r.getStatusCode().value(),r.getBody());
        for(var f:datos(r).path("familias"))if(f.path("codigo").asText().equals(c.f().getCodigoCaso()))return f;
        return null;
    }
    @Test void interrupcionHistoricaNoProhibeElParTrasAmbasRevisiones(){
        var c=caso();long id=interrumpida(c);var v=vinculaciones.findById(id).orElseThrow();long decision=v.getDecision().getId();
        String contexto=jdbc.queryForObject("select contexto_scoring::text from decisiones_comite where id=?",String.class,decision);
        assertNull(candidato(c));
        var parcial=liberar(id,"NINO");assertEquals("DISPONIBLE",parcial.path("cierre").path("estadoInfantil").asText());assertEquals("PENDIENTE_REVISION",parcial.path("cierre").path("estadoFamiliar").asText());
        assertNull(candidato(c));assertEquals(409,aceptar(c.n(),c.f()).getStatusCode().value());
        liberar(id,"FAMILIA");assertNotNull(candidato(c));assertEquals(200,aceptar(c.n(),c.f()).getStatusCode().value());
        assertEquals(contexto,jdbc.queryForObject("select contexto_scoring::text from decisiones_comite where id=?",String.class,decision));
        assertEquals(1,jdbc.queryForObject("select count(*) from interrupciones_convivencia where vinculacion_id=?",Integer.class,id));
        assertEquals(2,jdbc.queryForObject("select count(*) from decisiones_comite where codigo_nino=? and decision='ACEPTADA'",Integer.class,c.n().getCodigoCaso()));
    }
    @Test void salvaguardaSqlImpideEludirUnaRevisionPendiente(){
        var c=caso();long id=interrumpida(c);liberar(id,"NINO");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into decisiones_comite(codigo_nino,codigo_familia,decision,motivo,funcionario,fecha,puntaje,version_pesos) values(?,?,'ACEPTADA','Intento sintético que debe rechazarse','HU19-CM',now(),50,1)",c.n().getCodigoCaso(),c.f().getCodigoCaso()));
        assertEquals(1,jdbc.queryForObject("select count(*) from decisiones_comite where codigo_nino=?",Integer.class,c.n().getCodigoCaso()));
    }
    @Test void restriccionExpresaSigueVigenteDespuesDeRevisarExpedientes(){
        var c=caso();long id=interrumpida(c);liberar(id,"NINO");liberar(id,"FAMILIA");
        var e=new CombinacionExcluida();e.codigoNino=c.n().getCodigoCaso();e.codigoFamilia=c.f().getCodigoCaso();e.origen="COMITE";e.responsable="HU19-CM";e.fecha=LocalDateTime.now();exclusiones.saveAndFlush(e);
        assertNull(candidato(c));assertEquals(409,aceptar(c.n(),c.f()).getStatusCode().value());assertTrue(exclusiones.existsById(e.id));
    }
    @Test void cierreConservaRespuestaYFechasParaFamiliaYComite(){
        var c=caso();long id=interrumpida(c);
        var institucional=datos(req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null));
        assertEquals("CERRADA",institucional.path("estado").asText());assertEquals("ACEPTADA",institucional.path("respuestaFamiliar").asText());
        assertEquals("TRABAJADOR_SOCIAL",institucional.path("seguimientoFamiliar").path("interrupcionRol").asText());
        assertFalse(institucional.path("seguimientoFamiliar").path("interrupcionRegistradaEl").isNull());
        var propias=datos(req("/api/vinculaciones/propias",HttpMethod.GET,c.token(),null));
        assertEquals("ACEPTADA",propias.get(0).path("respuestaFamiliar").asText());assertFalse(propias.get(0).has("expedienteInfantil"));
    }
    @Test void citasDistinguenCierreDePropuestaNoAceptadaYFamiliaElegible(){
        var c=caso();long id=asignar(c);
        var pendiente=datos(req("/api/citas/opciones",HttpMethod.GET,c.token(),null)).path("vinculaciones").get(0);
        assertTrue(pendiente.path("motivoNoDisponible").asText().contains("aceptada"));assertFalse(pendiente.path("habilitada").asBoolean());
        req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR"));
        var activa=datos(req("/api/citas/opciones",HttpMethod.GET,c.token(),null)).path("vinculaciones").get(0);
        assertTrue(activa.path("habilitada").asBoolean());assertEquals("",activa.path("motivoNoDisponible").asText());
        assertEquals(200,req("/api/vinculaciones/"+id+"/cierre",HttpMethod.POST,comite,Map.of("confirmacionAutoridad",true,"motivo","Cierre sintético sin nuevas solicitudes permitidas")).getStatusCode().value());
        var cerrada=datos(req("/api/citas/opciones",HttpMethod.GET,c.token(),null)).path("vinculaciones").get(0);
        assertFalse(cerrada.path("habilitada").asBoolean());assertTrue(cerrada.path("motivoNoDisponible").asText().contains("cerrada"));
    }
    @Test void calculoNuevoUsaActualizacionFamiliarSinReescribirDecisionAnterior(){
        var c=caso();c.n().setCondicionesSalud(List.of(CondicionSalud.ASMA));ninos.saveAndFlush(c.n());
        int anterior=candidato(c).path("puntaje").asInt();
        var solicitud=req("/api/scoring/casos/"+c.n().getCodigoCaso()+"/decisiones",HttpMethod.POST,comite,Map.of("familia",c.f().getCodigoCaso(),"decision","REEVALUACION","motivo","Actualizar la capacidad de acompañamiento médico de la familia"));assertEquals(200,solicitud.getStatusCode().value(),solicitud.getBody());
        var decision=decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).get(0);String contexto=decision.getContextoScoring();assertNull(candidato(c));
        var entrevista=Map.<String,Object>of("tipoFamilia","PAREJA","redApoyo",List.of("FAMILIA_EXTENDIDA"),"provincia","PANAMA","disponibilidadViaje","ALTA","edadMinimaAceptada",1,"edadMaximaAceptada",12,"disposicionHermanos","DISPUESTA","condicionesPuedeAcompanarHoy",List.of("ASMA"));
        var cambio=req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),entrevista);assertEquals(200,cambio.getStatusCode().value(),cambio.getBody());assertTrue(datos(cambio).path("perfilCompleto").asBoolean(),cambio.getBody());
        var actual=candidato(c);assertNotNull(actual);assertTrue(actual.path("puntaje").asInt()>anterior);assertEquals("EVIDENCIA_TEXTO_7",actual.path("reglas").asText());
        assertEquals(contexto,decisiones.findById(decision.getId()).orElseThrow().getContextoScoring());assertEquals(1,decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).size());
    }
}
