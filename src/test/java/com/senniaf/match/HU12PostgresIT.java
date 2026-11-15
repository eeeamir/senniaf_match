package com.senniaf.match;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class HU12PostgresIT extends SoportePostgresIT {
    static final List<String> CAMPOS=List.of("apoyoIdiomas","respetoTradiciones","recursosComunitarios","respetoPreferencias","compromisoCultural");
    @SuppressWarnings("unchecked") Map<String,Object> entrevista(Caso c){return json.convertValue(datos(req("/api/familias/mi-perfil/detalle",HttpMethod.GET,c.token(),null)),Map.class);}
    String apoyo(Caso c,String campo){return datos(req("/api/familias/mi-perfil/detalle",HttpMethod.GET,c.token(),null)).path(campo).asText();}
    void antecedente(Caso c,String texto){var f=familias.findById(c.f().getId()).orElseThrow();f.setContinuidadCultural(texto);familias.saveAndFlush(f);}
    @Test void registrarEditarYConsultarCincoCamposSinCambiarPuntuacion(){
        Caso c=caso();var antes=scoring.candidato(c.n(),c.f(),scoring.pesos());var original=entrevista(c);
        var cuerpo=new LinkedHashMap<>(original);
        for(String campo:CAMPOS)cuerpo.put(campo,"Respuesta inicial independiente para "+campo);
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());
        for(String campo:CAMPOS)assertEquals(cuerpo.get(campo),apoyo(c,campo));
        cuerpo.put("apoyoIdiomas","Buscaremos apoyo comunitario para acompañar la comunicación.");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());
        var actual=familias.findById(c.f().getId()).orElseThrow();assertEquals(antes,scoring.candidato(c.n(),actual,scoring.pesos()));assertTrue(actual.isPerfilCompleto());assertEquals(0,actual.getNumeroReevaluaciones());
        var com=req("/api/scoring/familias/"+c.f().getCodigoCaso()+"/expediente",HttpMethod.GET,comite,null);assertEquals(200,com.getStatusCode().value());
        for(String campo:CAMPOS)assertEquals(cuerpo.get(campo),datos(com).path(campo).asText());
        var eventos=auditoria.findByCodigoCasoOrderByFechaAsc(c.f().getCodigoCaso());assertTrue(eventos.stream().anyMatch(e->e.getDescripcion().contains("Respuesta inicial independiente")));assertTrue(eventos.stream().anyMatch(e->e.getDescripcion().contains("Buscaremos apoyo comunitario")));
        var ultimo=eventos.get(eventos.size()-1);try{var cambios=json.readTree(ultimo.getDescripcion()).path("cambios");assertEquals(1,cambios.size());assertTrue(cambios.has("apoyoIdiomas"));}catch(java.io.IOException e){throw new AssertionError(e);}
        assertEquals(original.get("codigoCaso"),entrevista(c).get("codigoCaso"));
    }
    @Test void clienteAnteriorConservaAntecedenteSinRepartirloNiSobrescribirlo(){
        Caso c=caso();String historico="Apoyo histórico íntegro, sin distribuir entre campos nuevos.";antecedente(c,historico);
        var cuerpo=entrevista(c);cuerpo.remove("continuidadCultural");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertEquals(historico,apoyo(c,"continuidadCultural"));
        for(String valor:Arrays.asList(null,"","Intento de reemplazo del antecedente")){
            cuerpo.put("continuidadCultural",valor);assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertEquals(historico,apoyo(c,"continuidadCultural"));
        }
        for(String campo:CAMPOS)assertNull(entrevista(c).get(campo));
        cuerpo.put("apoyoIdiomas","Respuesta estructurada que se conserva si el cliente omite el campo");
        req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo);cuerpo.remove("apoyoIdiomas");req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo);
        assertTrue(apoyo(c,"apoyoIdiomas").startsWith("Respuesta estructurada"));
    }
    @Test void longitudInvalidaNoGuardaYBorradoExplicitoConservaAuditoria(){
        Caso c=caso();var cuerpo=entrevista(c);cuerpo.put("apoyoIdiomas","Apoyo inicial conservado en la auditoría familiar");req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo);
        for(String campo:CAMPOS){
            var invalido=new HashMap<>(cuerpo);invalido.put(campo,"a".repeat(10001));var respuesta=req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),invalido);
            assertEquals(400,respuesta.getStatusCode().value());assertEquals("Cada apoyo cultural admite hasta 10.000 caracteres",datos(respuesta).path("errores").path(campo).asText());
        }
        assertTrue(apoyo(c,"apoyoIdiomas").startsWith("Apoyo inicial"));
        cuerpo.put("apoyoIdiomas","");assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,c.token(),cuerpo).getStatusCode().value());assertEquals("",apoyo(c,"apoyoIdiomas"));assertTrue(auditoria.findByCodigoCasoOrderByFechaAsc(c.f().getCodigoCaso()).stream().anyMatch(e->e.getDescripcion().contains("Apoyo inicial")));
    }
    @Test void rolesYOtraFamiliaNoAlteranPerfilAjeno(){
        Caso c=caso(),otra=caso();var cuerpo=entrevista(otra);cuerpo.put("codigoCaso",c.f().getCodigoCaso());cuerpo.put("registradoPor",c.f().getRegistradoPor());cuerpo.put("apoyoIdiomas","Información que pertenece únicamente a la segunda familia");
        assertEquals(200,req("/api/familias/mi-perfil",HttpMethod.PUT,otra.token(),cuerpo).getStatusCode().value());assertFalse(apoyo(c,"apoyoIdiomas").contains("segunda familia"));assertTrue(apoyo(otra,"apoyoIdiomas").contains("segunda familia"));
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
