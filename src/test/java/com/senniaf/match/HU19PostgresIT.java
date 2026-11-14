package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.service.ScoringService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Solo se habilita expresamente en una base desechable hu19_ y con esquema SQL migrado. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HU19PostgresIT extends SoportePostgresIT {
    @Test void respuestaPersisteYExpedientesSeConservan(){
        Caso c=caso();long totalN=ninos.count(),totalF=familias.count(),id=asignar(c);
        var r=req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR","estado","CERRADA","respondidoPor","OTRO"));assertEquals(200,r.getStatusCode().value(),r.getBody());
        assertEquals(totalN,ninos.count());assertEquals(totalF,familias.count());
        var guardada=vinculaciones.findById(id).orElseThrow();assertEquals(Vinculacion.Estado.RESPUESTA_ACEPTADA,guardada.getEstado());assertEquals(c.f().getRegistradoPor(),guardada.getRespondidoPor());assertTrue(guardada.isReservaVigente());
        var detalle=req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null);assertEquals(200,detalle.getStatusCode().value());assertEquals("RESPUESTA_ACEPTADA",datos(detalle).path("estado").asText());assertEquals(2,datos(detalle).path("historial").size());
        assertEquals(409,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","RECHAZAR")).getStatusCode().value());
    }
    @Test void rechazoMantieneReservaSinPenalizarYNoExponeInfancia(){
        Caso c=caso();long id=asignar(c);var antes=scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje");
        assertEquals(403,req("/api/ninos/"+c.n().getCodigoCaso(),HttpMethod.GET,c.token(),null).getStatusCode().value());
        assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","RECHAZAR")).getStatusCode().value());
        for(String ruta:List.of("/api/ninos/"+c.n().getCodigoCaso(),"/api/ninos/"+c.n().getCodigoCaso()+"/auditoria","/api/vinculaciones/"+id,"/api/scoring/familias/"+c.f().getCodigoCaso()+"/expediente"))assertEquals(403,req(ruta,HttpMethod.GET,c.token(),null).getStatusCode().value(),ruta);
        var propia=req("/api/vinculaciones/propias",HttpMethod.GET,c.token(),null);assertFalse(propia.getBody().contains(c.n().getCodigoCaso()));assertFalse(propia.getBody().contains("motivo"));
        assertTrue(vinculaciones.findById(id).orElseThrow().isReservaVigente());assertFalse(familias.findById(c.f().getId()).orElseThrow().isReevaluacionPendiente());assertFalse(ninos.findById(c.n().getId()).orElseThrow().isBajaLogica());assertEquals(antes,scoring.candidato(c.n(),familias.findById(c.f().getId()).orElseThrow(),scoring.pesos()).get("puntaje"));assertEquals(409,aceptar(c.n(),c.f()).getStatusCode().value());
    }
    @Test void permisosRealesYFamiliaAjena(){
        Caso c=caso(),otra=caso();long id=asignar(c);
        for(String token:List.of(comite,social,psicologo))assertEquals(200,req("/api/vinculaciones/"+id,HttpMethod.GET,token,null).getStatusCode().value());
        for(String token:List.of(otro,otra.token(),c.token()))assertEquals(403,req("/api/vinculaciones/"+id,HttpMethod.GET,token,null).getStatusCode().value());
        for(String token:List.of(comite,social,psicologo,otra.token()))assertEquals(403,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,token,Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        assertEquals(403,req("/api/vinculaciones",HttpMethod.GET,c.token(),null).getStatusCode().value());assertEquals(401,req("/api/vinculaciones",HttpMethod.GET,null,null).getStatusCode().value());
        HttpHeaders h=new HttpHeaders();h.setBearerAuth(c.token());h.set("X-User-Role","COMITE");h.set("X-User-Id","HU19-CM");assertEquals(403,http.exchange("/api/vinculaciones",HttpMethod.GET,new HttpEntity<>(h),String.class).getStatusCode().value());
    }
    @Test void dosFamiliasNoReservanElMismoNino() throws Exception {Caso a=caso(),b=caso();assertEquals(List.of(200,409),simultaneas(()->aceptar(a.n(),a.f()),()->aceptar(a.n(),b.f())));assertEquals(1,decisiones.findByCodigoNinoOrderByFechaAsc(a.n().getCodigoCaso()).size());}
    @Test void dosNinosNoReservanLaMismaFamilia() throws Exception {Caso a=caso(),b=caso();assertEquals(List.of(200,409),simultaneas(()->aceptar(a.n(),a.f()),()->aceptar(b.n(),a.f())));assertEquals(1,vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(a.f().getRegistradoPor()).size());}
    @Test void dobleAceptacionNoDuplicaVinculacion() throws Exception {Caso c=caso();assertEquals(List.of(200,409),simultaneas(()->aceptar(c.n(),c.f()),()->aceptar(c.n(),c.f())));assertEquals(1,vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(c.f().getRegistradoPor()).size());}
    @Test void respuestasContradictoriasSeSerializan() throws Exception {Caso c=caso();long id=asignar(c);String ruta="/api/vinculaciones/"+id+"/respuesta";assertEquals(List.of(200,409),simultaneas(()->req(ruta,HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")),()->req(ruta,HttpMethod.POST,c.token(),Map.of("respuesta","RECHAZAR"))));assertEquals(2,datos(req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null)).path("historial").size());}
    @Test void disponibilidadDependeDeReservaNoDeAceptacionHistorica(){
        Caso c=caso();long id=asignar(c);
        assertEquals(true,scoring.caso(c.n().getCodigoCaso()).get("propuestaAceptada"));
        // Simula exclusivamente en esta base desechable una liberación institucional futura.
        jdbc.update("update vinculaciones set reserva_vigente=false where id=?",id);
        var actual=scoring.caso(c.n().getCodigoCaso());
        assertEquals(false,actual.get("propuestaAceptada"));
        assertTrue(((List<?>)actual.get("familias")).stream().anyMatch(f->c.f().getCodigoCaso().equals(((Map<?,?>)f).get("codigo"))));
        assertTrue(decisiones.existsByCodigoNinoAndDecision(c.n().getCodigoCaso(),"ACEPTADA"));
    }

}
