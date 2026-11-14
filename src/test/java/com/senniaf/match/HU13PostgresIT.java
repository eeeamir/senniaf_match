package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class HU13PostgresIT extends SoportePostgresIT {
    static final List<String> FACTORES=List.of("edad","hermanos","salud","apoyo","logistica");
    Map<String,Integer> originales;
    JsonNode pesos(){var r=req("/api/scoring/pesos",HttpMethod.GET,comite,null);assertEquals(200,r.getStatusCode().value(),r.getBody());return datos(r);}
    Map<String,Integer> valores(JsonNode p){var m=new LinkedHashMap<String,Integer>();FACTORES.forEach(k->m.put(k,p.path(k).asInt()));return m;}
    Map<String,Object> propuesta(JsonNode p){
        var m=new LinkedHashMap<String,Object>(valores(p));String origen=FACTORES.stream().filter(k->p.path(k).asInt()>0).findFirst().orElseThrow();String destino=FACTORES.stream().filter(k->!k.equals(origen)).findFirst().orElseThrow();
        m.put(origen,p.path(origen).asInt()-1);m.put(destino,p.path(destino).asInt()+1);m.put("versionEsperada",p.path("version").asInt());m.put("justificacion","Ajuste sintético revisado por el Comité durante la prueba HU13");return m;
    }
    JsonNode historial(){return datos(req("/api/scoring/pesos/historial",HttpMethod.GET,comite,null));}
    @BeforeEach void conservarConfiguracion(){originales=valores(pesos());}
    @AfterEach void restablecerValoresConAuditoria(){
        var p=pesos();if(originales.equals(valores(p)))return;
        var restaurar=new LinkedHashMap<String,Object>(originales);restaurar.put("versionEsperada",p.path("version").asInt());restaurar.put("justificacion","Restablecer los valores sintéticos previos a la prueba HU13, conservando el historial");
        var r=req("/api/scoring/pesos",HttpMethod.PUT,comite,restaurar);assertEquals(200,r.getStatusCode().value(),r.getBody());assertEquals(originales,valores(pesos()));
    }
    @Test void cambioJustificadoConservaVersionesResponsableFechaYDecisionHistorica(){
        Caso c=caso();solicitarRevision(c);var d=decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).get(0);String contexto=d.getContextoScoring();int puntaje=d.getPuntaje(),versionHistorica=d.getVersionPesos();
        var antes=pesos();int n=historial().size();var cuerpo=propuesta(antes);var r=req("/api/scoring/pesos",HttpMethod.PUT,comite,cuerpo);assertEquals(200,r.getStatusCode().value(),r.getBody());
        var despues=pesos();assertEquals(antes.path("version").asInt()+1,despues.path("version").asInt());FACTORES.forEach(k->assertEquals(cuerpo.get(k),despues.path(k).asInt()));
        var h=historial();assertEquals(n+1,h.size());var evento=h.get(h.size()-1);assertEquals("HU19-CM",evento.path("autor").asText());assertFalse(evento.path("fecha").asText().isBlank());assertEquals("COMITE",evento.path("cambio").path("rol").asText());
        assertEquals(json.valueToTree(valores(antes)),evento.path("cambio").path("anteriores"));assertEquals(json.valueToTree(valores(despues)),evento.path("cambio").path("nuevos"));assertEquals(cuerpo.get("justificacion"),evento.path("cambio").path("justificacion").asText());assertEquals(antes.path("version").asInt(),evento.path("cambio").path("versionAnterior").asInt());assertEquals(despues.path("version").asInt(),evento.path("cambio").path("versionNueva").asInt());
        var conservada=decisiones.findById(d.getId()).orElseThrow();assertEquals(contexto,conservada.getContextoScoring());assertEquals(puntaje,conservada.getPuntaje());assertEquals(versionHistorica,conservada.getVersionPesos());
    }
    void solicitarRevision(Caso c){var r=req("/api/scoring/casos/"+c.n().getCodigoCaso()+"/decisiones",HttpMethod.POST,comite,Map.of("familia",c.f().getCodigoCaso(),"decision","REEVALUACION","motivo","Conservar el fundamento histórico antes del ajuste sintético"));assertEquals(200,r.getStatusCode().value(),r.getBody());}
    @Test void rechazaValoresInvalidosSinModificarPesosNiAuditoria(){
        var antes=pesos();var historia=historial();var casos=new ArrayList<Map<String,Object>>();
        for(Object valor:Arrays.asList(-1,101,20.5,"20",true,null)){var c=propuesta(antes);c.put("edad",valor);casos.add(c);}
        var suma=propuesta(antes);suma.put("edad",((Integer)suma.get("edad"))+1);casos.add(suma);
        var falta=propuesta(antes);falta.remove("salud");casos.add(falta);
        var extra=propuesta(antes);extra.put("cultura",0);casos.add(extra);
        for(Object razon:Arrays.asList("breve"," ".repeat(15),"a".repeat(301),null,123)){var c=propuesta(antes);c.put("justificacion",razon);casos.add(c);}
        for(Object version:Arrays.asList(null,"1",1.5,true)){var c=propuesta(antes);c.put("versionEsperada",version);casos.add(c);}
        for(var c:casos){var r=req("/api/scoring/pesos",HttpMethod.PUT,comite,c);assertEquals(400,r.getStatusCode().value(),r.getBody());assertFalse(r.getBody().contains("org.postgresql"));assertEquals(antes,pesos());assertEquals(historia,historial());}
    }
    @Test void versionesDesactualizadasNoSobrescribenCambios(){
        var antes=pesos();var cambio=propuesta(antes);assertEquals(200,req("/api/scoring/pesos",HttpMethod.PUT,comite,cambio).getStatusCode().value());var vigente=pesos();var h=historial();
        var r=req("/api/scoring/pesos",HttpMethod.PUT,comite,cambio);assertEquals(409,r.getStatusCode().value());assertTrue(datos(r).path("mensaje").asText().contains("Recarga"));assertEquals(vigente,pesos());assertEquals(h,historial());
    }
    @Test void servidorRechazaOtrosRolesInclusoSiFalsificanCabeceras(){
        Caso c=caso();var antes=pesos();var h=historial();
        for(String token:List.of(social,psicologo,c.token())){
            assertEquals(403,req("/api/scoring/pesos",HttpMethod.GET,token,null).getStatusCode().value());assertEquals(403,req("/api/scoring/pesos/historial",HttpMethod.GET,token,null).getStatusCode().value());
            HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);headers.setBearerAuth(token);headers.set("X-User-Role","COMITE");headers.set("X-User-Id","HU19-CM");
            assertEquals(403,http.exchange("/api/scoring/pesos",HttpMethod.PUT,new HttpEntity<>(propuesta(antes),headers),String.class).getStatusCode().value());
        }
        assertEquals(401,req("/api/scoring/pesos",HttpMethod.PUT,null,propuesta(antes)).getStatusCode().value());assertEquals(antes,pesos());assertEquals(h,historial());
    }
    @Test void dosCambiosSimultaneosSoloConfirmanUnaVersion() throws Exception {
        var antes=pesos();int n=historial().size();var a=propuesta(antes);var b=new LinkedHashMap<>(a);b.put("justificacion","Segunda propuesta sintética simultánea que debe recargar su versión");
        assertEquals(List.of(200,409),simultaneas(()->req("/api/scoring/pesos",HttpMethod.PUT,comite,a),()->req("/api/scoring/pesos",HttpMethod.PUT,comite,b)));
        assertEquals(antes.path("version").asInt()+1,pesos().path("version").asInt());assertEquals(n+1,historial().size());
    }
}
