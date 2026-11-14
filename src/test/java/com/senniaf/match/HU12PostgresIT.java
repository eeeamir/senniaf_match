package com.senniaf.match;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class HU12PostgresIT extends SoportePostgresIT {
    @SuppressWarnings("unchecked") Map<String,Object> entrevista(Caso c){return json.convertValue(datos(req("/api/familias/mi-perfil/detalle",HttpMethod.GET,c.token(),null)),Map.class);}
    String apoyos(Caso c){return datos(req("/api/familias/mi-perfil/detalle",HttpMethod.GET,c.token(),null)).path("continuidadCultural").asText();}
    @Test void registrarEditarYConsultarSinCambiarPuntuacion(){
        Caso c=caso();var antes=scoring.candidato(c.n(),c.f(),scoring.pesos());var original=entrevista(c);
        var cuerpo=new LinkedHashMap<>(original);cuerpo.put("continuidadCultural","Podemos acompañar el idioma familiar y los vínculos con su comunidad.");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertEquals(cuerpo.get("continuidadCultural"),apoyos(c));
        cuerpo.put("continuidadCultural","Buscaremos apoyo comunitario para mantener sus costumbres e idioma.");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());
        var actual=familias.findById(c.f().getId()).orElseThrow();assertEquals(antes,scoring.candidato(c.n(),actual,scoring.pesos()));assertTrue(actual.isPerfilCompleto());assertEquals(0,actual.getNumeroReevaluaciones());
        var com=req("/api/scoring/familias/"+c.f().getCodigoCaso()+"/expediente",HttpMethod.GET,comite,null);assertEquals(200,com.getStatusCode().value());assertEquals(cuerpo.get("continuidadCultural"),datos(com).path("continuidadCultural").asText());
        var eventos=auditoria.findByCodigoCasoOrderByFechaAsc(c.f().getCodigoCaso());assertTrue(eventos.stream().anyMatch(e->e.getDescripcion().contains("Podemos acompañar el idioma")));assertTrue(eventos.stream().anyMatch(e->e.getDescripcion().contains("Buscaremos apoyo comunitario")));
        assertEquals(original.get("codigoCaso"),entrevista(c).get("codigoCaso"));
    }
    @Test void clienteAnteriorQueOmiteCampoConservaAntecedentes(){
        Caso c=caso();var cuerpo=entrevista(c);cuerpo.put("continuidadCultural","Apoyo histórico que no debe borrarse si el cliente no envía el campo.");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());cuerpo.remove("continuidadCultural");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertTrue(apoyos(c).startsWith("Apoyo histórico"));
        cuerpo.put("continuidadCultural",null);assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertTrue(apoyos(c).startsWith("Apoyo histórico"));
    }
    @Test void longitudInvalidaNoGuardaYBorradoExplicitoConservaAuditoria(){
        Caso c=caso();var cuerpo=entrevista(c);cuerpo.put("continuidadCultural","Apoyo inicial conservado en la auditoría familiar");req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo);
        cuerpo.put("continuidadCultural","a".repeat(2001));var invalida=req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo);assertEquals(400,invalida.getStatusCode().value());assertEquals("Los apoyos pueden tener hasta 2000 caracteres",datos(invalida).path("errores").path("continuidadCultural").asText());assertTrue(apoyos(c).startsWith("Apoyo inicial"));
        cuerpo.put("continuidadCultural","");assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertEquals("",apoyos(c));assertTrue(auditoria.findByCodigoCasoOrderByFechaAsc(c.f().getCodigoCaso()).stream().anyMatch(e->e.getDescripcion().contains("Apoyo inicial")));
    }
    @Test void rolesYOtraFamiliaNoAlteranPerfilAjeno(){
        Caso c=caso(),otra=caso();var cuerpo=entrevista(otra);cuerpo.put("codigoCaso",c.f().getCodigoCaso());cuerpo.put("registradoPor",c.f().getRegistradoPor());cuerpo.put("continuidadCultural","Información que pertenece únicamente a la segunda familia");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,otra.token(),cuerpo).getStatusCode().value());assertFalse(apoyos(c).contains("segunda familia"));assertTrue(apoyos(otra).contains("segunda familia"));
        for(String token:List.of(social,psicologo,comite))assertEquals(403,req("/api/familias/mi-perfil",HttpMethod.PUT,token,cuerpo).getStatusCode().value());
        for(String token:List.of(social,psicologo,otra.token()))assertEquals(403,req("/api/scoring/familias/"+c.f().getCodigoCaso()+"/expediente",HttpMethod.GET,token,null).getStatusCode().value());
    }
    @Test void altaFamiliarIncluyeApoyosEnCampoExistente(){
        String actor="UI12-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);usuario(actor,"FAMILIA");String token=login(actor);
        var cuerpo=new HashMap<>(entrevista(caso()));cuerpo.put("continuidadCultural","Apoyos culturales registrados en la entrevista inicial");
        var alta=req("/api/familias",HttpMethod.POST,token,cuerpo);assertEquals(201,alta.getStatusCode().value(),alta.getBody());
        assertEquals(cuerpo.get("continuidadCultural"),datos(req("/api/familias/mi-perfil/detalle",HttpMethod.GET,token,null)).path("continuidadCultural").asText());
    }
}
