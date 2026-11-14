package com.senniaf.match;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class HU11PostgresIT extends SoportePostgresIT {
    Map<String,Object> informe(){return Map.of("fechaInterrupcion",LocalDate.now(ZoneId.of("America/Panama")).toString(),"referenciaActuacion","ACT-HU11-PRUEBA","justificacion","Se registra una interrupción sintética para revisión institucional");}
    long aceptada(Caso c){long id=asignar(c);assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());return id;}
    @Test void profesionalRegistraYComiteRevisaSinSanciones(){
        Caso c=caso();long id=aceptada(c);int puntaje=(int)scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje");
        var registro=req("/api/vinculaciones/"+id+"/interrupcion",HttpMethod.POST,social,informe());assertEquals(200,registro.getStatusCode().value(),registro.getBody());
        var i=datos(registro).path("interrupcion");assertEquals("HU19-TS",i.path("registradoPor").asText());assertTrue(datos(registro).path("interrupcionPendiente").asBoolean());
        var revision=req("/api/vinculaciones/"+id+"/interrupcion/revision",HttpMethod.POST,comite,Map.of("justificacion","Se revisaron los antecedentes sintéticos y se documentó el seguimiento"));assertEquals(200,revision.getStatusCode().value(),revision.getBody());
        assertFalse(datos(revision).path("interrupcionPendiente").asBoolean());assertEquals(i.path("justificacion"),datos(revision).path("interrupcion").path("justificacion"));
        assertEquals("HU19-CM",datos(revision).path("interrupcion").path("revisadoPor").asText());
        assertTrue(vinculaciones.findById(id).orElseThrow().isReservaVigente());assertFalse(ninos.findById(c.n().getId()).orElseThrow().isBajaLogica());assertFalse(ninos.findById(c.n().getId()).orElseThrow().isCierrePorAdopcion());
        assertEquals(puntaje,scoring.candidato(c.n(),familias.findById(c.f().getId()).orElseThrow(),scoring.pesos()).get("puntaje"));
        assertEquals(4,datos(req("/api/vinculaciones/"+id,HttpMethod.GET,psicologo,null)).path("historial").size());
        var propia=req("/api/vinculaciones/propias",HttpMethod.GET,c.token(),null);assertFalse(propia.getBody().contains("ACT-HU11-PRUEBA"));assertFalse(propia.getBody().contains("interrupción sintética"));
        assertEquals(409,req("/api/vinculaciones/"+id+"/interrupcion/revision",HttpMethod.POST,comite,Map.of("justificacion","No debe reemplazar la revisión original registrada")).getStatusCode().value());
    }
    @Test void rolesYAsignacionSeCompruebanEnServidor(){
        Caso c=caso();long id=aceptada(c);String ruta="/api/vinculaciones/"+id+"/interrupcion";
        for(String token:List.of(comite,c.token(),otro))assertEquals(403,req(ruta,HttpMethod.POST,token,informe()).getStatusCode().value());
        assertEquals(200,req(ruta,HttpMethod.POST,psicologo,informe()).getStatusCode().value());
        assertEquals(403,req("/api/vinculaciones/"+id,HttpMethod.GET,otro,null).getStatusCode().value());
        for(String token:List.of(social,psicologo,c.token(),otro))assertEquals(403,req(ruta+"/revision",HttpMethod.POST,token,Map.of("justificacion","Revisión que este rol no puede registrar")).getStatusCode().value());
    }
    @Test void pendienteORechazadaNoEsConvivencia(){
        Caso c=caso();long id=asignar(c);String ruta="/api/vinculaciones/"+id+"/interrupcion";
        assertEquals(409,req(ruta,HttpMethod.POST,social,informe()).getStatusCode().value());
        req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","RECHAZAR"));
        assertEquals(409,req(ruta,HttpMethod.POST,social,informe()).getStatusCode().value());
    }
    @Test void camposInvalidosNoCreanRegistros(){
        Caso c=caso();long id=aceptada(c);String ruta="/api/vinculaciones/"+id+"/interrupcion";
        for(Map<String,Object> cambios:List.of(Map.<String,Object>of("justificacion","corto"),Map.<String,Object>of("referenciaActuacion"," "),Map.<String,Object>of("fechaInterrupcion",LocalDate.now(ZoneId.of("America/Panama")).plusDays(1).toString()))){
            var cuerpo=new HashMap<>(informe());cuerpo.putAll(cambios);assertEquals(400,req(ruta,HttpMethod.POST,social,cuerpo).getStatusCode().value());
        }
        assertTrue(datos(req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null)).path("interrupcion").isNull());
    }
    @Test void solicitudesSimultaneasNoDuplicanInterrupcion() throws Exception {
        Caso c=caso();long id=aceptada(c);String ruta="/api/vinculaciones/"+id+"/interrupcion";
        assertEquals(List.of(200,409),simultaneas(()->req(ruta,HttpMethod.POST,social,informe()),()->req(ruta,HttpMethod.POST,psicologo,informe())));
    }
    @Test void revisionesSimultaneasConservanUnaSolaActuacion() throws Exception {
        Caso c=caso();long id=aceptada(c);String ruta="/api/vinculaciones/"+id+"/interrupcion";
        assertEquals(200,req(ruta,HttpMethod.POST,social,informe()).getStatusCode().value());
        assertEquals(List.of(200,409),simultaneas(()->req(ruta+"/revision",HttpMethod.POST,comite,Map.of("justificacion","Primera revisión sintética de los antecedentes")),()->req(ruta+"/revision",HttpMethod.POST,comite,Map.of("justificacion","Segunda revisión simultánea que no debe sobrescribir"))));
    }
}
