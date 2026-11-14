package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.senniaf.match.model.EventoCaso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class HU14PostgresIT extends SoportePostgresIT {
    @SuppressWarnings("unchecked") Map<String,Object> entrevista(Caso c){return json.convertValue(datos(req("/api/familias/mi-perfil/detalle",HttpMethod.GET,c.token(),null)),Map.class);}
    void solicitar(Caso c){var r=req("/api/scoring/casos/"+c.n().getCodigoCaso()+"/decisiones",HttpMethod.POST,comite,Map.of("familia",c.f().getCodigoCaso(),"decision","REEVALUACION","motivo","Ampliar la entrevista para una revisión humana del caso"));assertEquals(200,r.getStatusCode().value(),r.getBody());}
    void completar(Caso c){var r=req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),entrevista(c));assertEquals(200,r.getStatusCode().value(),r.getBody());}
    List<JsonNode> alertas(Caso c){var r=req("/api/scoring/reevaluaciones",HttpMethod.GET,comite,null);assertEquals(200,r.getStatusCode().value(),r.getBody());var propias=new ArrayList<JsonNode>();datos(r).forEach(a->{if(a.path("codigoFamilia").asText().equals(c.f().getCodigoCaso()))propias.add(a);});return propias;}
    long completadas(JsonNode alerta){long n=0;for(var e:alerta.path("antecedentes"))if(e.path("completada").asBoolean())n++;return n;}
    @Test void umbralCeroUnoDosTresYMasSoloCuentaCorreccionesCompletas(){
        Caso c=caso();var puntuacion=scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje");assertTrue(alertas(c).isEmpty());
        for(int i=1;i<=4;i++){
            solicitar(c);assertEquals(i-1,familias.findById(c.f().getId()).orElseThrow().getNumeroReevaluaciones());
            if(i<=3)assertTrue(alertas(c).isEmpty());else assertEquals(3,alertas(c).get(0).path("completadas").asInt());
            completar(c);var f=familias.findById(c.f().getId()).orElseThrow();assertEquals(i,f.getNumeroReevaluaciones());assertFalse(f.isReevaluacionPendiente());assertTrue(f.isPerfilCompleto());
            assertEquals(puntuacion,scoring.candidato(c.n(),f,scoring.pesos()).get("puntaje"));
            if(i<3)assertTrue(alertas(c).isEmpty());else {var a=alertas(c);assertEquals(1,a.size());assertEquals(i,a.get(0).path("completadas").asInt());assertEquals(i,completadas(a.get(0)));assertEquals(0,a.get(0).path("completadasSinFechaDetallada").asInt());for(var e:a.get(0).path("antecedentes")){assertFalse(e.path("fecha").isNull());assertFalse(e.path("responsable").asText().isBlank());}}
        }
    }
    @Test void entrevistaIncompletaYSolicitudesSinCompletarNoAumentanContador(){
        Caso c=caso();solicitar(c);var incompleta=entrevista(c);incompleta.remove("provincia");
        assertEquals(400,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),incompleta).getStatusCode().value());
        var f=familias.findById(c.f().getId()).orElseThrow();assertTrue(f.isReevaluacionPendiente());assertEquals(0,f.getNumeroReevaluaciones());assertTrue(alertas(c).isEmpty());
        // Un antecedente cancelado histórico tampoco acredita una corrección completada.
        EventoCaso cancelada=new EventoCaso();cancelada.setCodigoCaso(f.getCodigoCaso());cancelada.setActor("HU19-CM");cancelada.setFecha(LocalDateTime.now());cancelada.setAccion("REEVALUACION_CANCELADA");cancelada.setDescripcion("Antecedente sintético cancelado, sin entrevista completada");auditoria.saveAndFlush(cancelada);
        assertTrue(alertas(c).isEmpty());assertEquals(0,familias.findById(f.getId()).orElseThrow().getNumeroReevaluaciones());
    }
    @Test void conservaContadorHistoricoSinInventarFechasNiDuplicarAvisos(){
        Caso c=caso();var f=familias.findById(c.f().getId()).orElseThrow();f.setNumeroReevaluaciones(3);familias.saveAndFlush(f);
        for(int i=0;i<2;i++){var a=alertas(c);assertEquals(1,a.size());assertEquals(3,a.get(0).path("completadas").asInt());assertEquals(3,a.get(0).path("completadasSinFechaDetallada").asInt());assertTrue(a.get(0).path("antecedentes").isEmpty());}
        solicitar(c);assertTrue(alertas(c).get(0).path("pendiente").asBoolean());assertEquals(3,alertas(c).get(0).path("completadas").asInt());assertEquals(0,completadas(alertas(c).get(0)));
    }
    @Test void dosEnviosSimultaneosCompletanUnaSolaReevaluacion() throws Exception {
        Caso c=caso();for(int i=0;i<2;i++){solicitar(c);completar(c);}solicitar(c);var cuerpo=entrevista(c);
        assertEquals(List.of(200,200),simultaneas(()->req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo),()->req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo)));
        assertEquals(3,familias.findById(c.f().getId()).orElseThrow().getNumeroReevaluaciones());assertEquals(1,alertas(c).size());assertEquals(3,completadas(alertas(c).get(0)));
    }
    @Test void servidorRestringeAlertasAlComite(){
        Caso c=caso();for(String token:List.of(social,psicologo,otro,c.token()))assertEquals(403,req("/api/scoring/reevaluaciones",HttpMethod.GET,token,null).getStatusCode().value());
        assertEquals(401,req("/api/scoring/reevaluaciones",HttpMethod.GET,null,null).getStatusCode().value());
    }
}
