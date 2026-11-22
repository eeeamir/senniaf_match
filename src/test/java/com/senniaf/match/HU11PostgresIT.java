package com.senniaf.match;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class HU11PostgresIT extends SoportePostgresIT {
    Map<String,Object> informe(){return Map.of("motivo","ADAPTACION","justificacion","Se registra una interrupción sintética para revisión institucional");}
    long aceptada(Caso c){long id=asignar(c);assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());autorizarInicio(id);return id;}
    @Test void profesionalRegistraYComiteRevisaSinSanciones(){
        Caso c=caso();long id=aceptada(c);int puntaje=(int)scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje");
        var cuerpo=new HashMap<>(informe());cuerpo.put("registradoPor","USUARIO-SUPLANTADO");cuerpo.put("rolProfesional","COMITE");cuerpo.put("fechaRegistro","2000-01-01T00:00:00");
        var registro=req("/api/vinculaciones/"+id+"/interrupcion",HttpMethod.POST,social,cuerpo);assertEquals(200,registro.getStatusCode().value(),registro.getBody());
        var i=datos(registro).path("interrupcion");assertEquals("HU19-TS",i.path("registradoPor").asText());assertTrue(datos(registro).path("interrupcionPendiente").asBoolean());assertEquals("ADAPTACION",i.path("motivo").asText());assertEquals("TRABAJADOR_SOCIAL",i.path("rolProfesional").asText());assertTrue(i.path("fechaInterrupcion").isNull());assertTrue(i.path("referenciaActuacion").isNull());assertFalse(i.path("fechaRegistro").isNull());assertFalse(i.path("fechaRegistro").asText().startsWith("2000-"));
        var revision=req("/api/vinculaciones/"+id+"/interrupcion/revision",HttpMethod.POST,comite,Map.of("justificacion","Se revisaron los antecedentes sintéticos y se documentó el seguimiento"));assertEquals(200,revision.getStatusCode().value(),revision.getBody());
        assertFalse(datos(revision).path("interrupcionPendiente").asBoolean());assertEquals(i.path("justificacion"),datos(revision).path("interrupcion").path("justificacion"));
        assertEquals("HU19-CM",datos(revision).path("interrupcion").path("revisadoPor").asText());
        assertTrue(vinculaciones.findById(id).orElseThrow().isReservaVigente());assertFalse(ninos.findById(c.n().getId()).orElseThrow().isBajaLogica());assertFalse(ninos.findById(c.n().getId()).orElseThrow().isCierrePorAdopcion());
        assertEquals(puntaje,scoring.candidato(c.n(),familias.findById(c.f().getId()).orElseThrow(),scoring.pesos()).get("puntaje"));
        assertEquals(6,datos(req("/api/vinculaciones/"+id,HttpMethod.GET,psicologo,null)).path("historial").size());
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
        for(Map<String,Object> cambios:List.of(Map.<String,Object>of("justificacion","corto"),Map.<String,Object>of("motivo"," "),Map.<String,Object>of("motivo","NO_VERIFICADO"))){
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
    @Test void autorizacionEInicioRealesSonRequisitosYNoSeSuplantan(){
        Caso c=caso();long id=asignar(c);String ruta="/api/vinculaciones/"+id;
        assertEquals(200,req(ruta+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        assertEquals(409,req(ruta+"/interrupcion",HttpMethod.POST,social,informe()).getStatusCode().value());
        var a=new HashMap<String,Object>();a.put("referencia","RESOLUCION-LOCAL-VALIDACION");a.put("fechaEmision",LocalDate.now(ZoneId.of("America/Panama")).toString());a.put("confirmacion",true);a.put("responsable","SUPLANTACION");a.put("rol","FAMILIA");a.put("registradaEl","2000-01-01T00:00:00Z");
        for(String token:List.of(c.token(),social,psicologo,otro))assertEquals(403,req(ruta+"/autorizacion-preadoptiva",HttpMethod.POST,token,a).getStatusCode().value());
        var invalida=new HashMap<>(a);invalida.put("fechaEmision",LocalDate.now(ZoneId.of("America/Panama")).plusDays(1).toString());assertEquals(400,req(ruta+"/autorizacion-preadoptiva",HttpMethod.POST,comite,invalida).getStatusCode().value());
        var registro=req(ruta+"/autorizacion-preadoptiva",HttpMethod.POST,comite,a);assertEquals(200,registro.getStatusCode().value(),registro.getBody());var dato=datos(registro).path("autorizacionPreadoptiva");assertEquals("HU19-CM",dato.path("responsable").asText());assertEquals("COMITE",dato.path("rol").asText());assertFalse(dato.path("registradaEl").asText().startsWith("2000"));assertTrue(dato.path("fechaInicioConvivencia").isNull());
        assertFalse(datos(req(ruta,HttpMethod.GET,social,null)).path("puedeReportarInterrupcion").asBoolean());assertEquals(409,req(ruta+"/interrupcion",HttpMethod.POST,social,informe()).getStatusCode().value());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into interrupciones_convivencia(vinculacion_id,motivo,justificacion,registrado_por,rol_profesional,fecha_registro,version) values (?,'OTRO','Prueba local de salvaguarda de recuperación','HU19-TS','TRABAJADOR_SOCIAL',CURRENT_TIMESTAMP,0)",id));
        a.put("fechaInicioConvivencia",LocalDate.now(ZoneId.of("America/Panama")).toString());assertEquals(200,req(ruta+"/autorizacion-preadoptiva",HttpMethod.POST,comite,a).getStatusCode().value());assertTrue(datos(req(ruta,HttpMethod.GET,social,null)).path("puedeReportarInterrupcion").asBoolean());
        var propia=datos(req("/api/vinculaciones/propias",HttpMethod.GET,c.token(),null)).get(0).path("autorizacionPreadoptiva");assertFalse(propia.has("referencia"));assertFalse(propia.has("responsable"));assertFalse(propia.path("fechaInicioConvivencia").isNull());
        assertEquals(409,req(ruta+"/autorizacion-preadoptiva",HttpMethod.POST,comite,a).getStatusCode().value());
        int puntaje=(int)scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje");assertEquals(200,req(ruta+"/interrupcion",HttpMethod.POST,psicologo,informe()).getStatusCode().value());
        assertFalse((boolean)scoring.antecedenteInterrupcion(c.f().getCodigoCaso()).get("revisada"));
        assertEquals(200,req(ruta+"/interrupcion/revision",HttpMethod.POST,comite,Map.of("justificacion","Revisión local de la interrupción con acompañamiento institucional")).getStatusCode().value());
        assertTrue((boolean)scoring.antecedenteInterrupcion(c.f().getCodigoCaso()).get("revisada"));assertTrue((boolean)scoring.antecedenteInterrupcion(c.f().getCodigoCaso()).get("cierrePendiente"));
        assertEquals(403,req(ruta+"/informacion-asignada",HttpMethod.GET,c.token(),null).getStatusCode().value());assertEquals(puntaje,scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje"));
        assertEquals(200,req(ruta+"/cierre",HttpMethod.POST,comite,Map.of("motivo","Cierre institucional confirmado solo en esta prueba local","confirmacionAutoridad",true)).getStatusCode().value());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into decisiones_comite(codigo_nino,codigo_familia,decision,motivo,funcionario,fecha,puntaje,version_pesos) values (?,?,'ACEPTADA','Nueva propuesta que no debe registrarse','HU19-CM',CURRENT_TIMESTAMP,50,1)",c.n().getCodigoCaso(),c.f().getCodigoCaso()));
        for(String expediente:List.of("NINO","FAMILIA"))assertEquals(200,req(ruta+"/disponibilidad",HttpMethod.POST,comite,Map.of("expediente",expediente,"justificacion","Revisión local separada de disponibilidad y elegibilidad")).getStatusCode().value());
        assertFalse((boolean)scoring.antecedenteInterrupcion(c.f().getCodigoCaso()).get("cierrePendiente"));
        var ranking=json.valueToTree(scoring.caso(c.n().getCodigoCaso())).path("familias");assertTrue(java.util.stream.StreamSupport.stream(ranking.spliterator(),false).anyMatch(f->c.f().getCodigoCaso().equals(f.path("codigo").asText())));
        assertEquals(200,aceptar(c.n(),c.f()).getStatusCode().value());
    }
    @Test void reevaluacionConservaApartadosEIndicaObservacionNatural(){
        Caso c=caso();var cuerpo=new HashMap<String,Object>();cuerpo.put("familia",c.f().getCodigoCaso());cuerpo.put("decision","REEVALUACION");cuerpo.put("motivo","Se solicita ampliar los apoyos culturales y la información del hogar.");cuerpo.put("apartados",List.of("Hogar y red de apoyo","Apoyos culturales y comunitarios"));cuerpo.put("categoria","INCOMPLETA");
        var r=req("/api/scoring/casos/"+c.n().getCodigoCaso()+"/decisiones",HttpMethod.POST,comite,cuerpo);assertEquals(200,r.getStatusCode().value(),r.getBody());
        var propia=datos(req("/api/familias/mi-perfil",HttpMethod.GET,c.token(),null));assertEquals(2,propia.path("apartadosReevaluacion").size());assertEquals(cuerpo.get("motivo"),propia.path("observacionReevaluacion").asText());assertFalse(propia.path("observacionReevaluacion").asText().contains("Apartados:"));assertEquals(0,propia.path("numeroReevaluaciones").asInt());
        var guardada=decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).get(0);assertEquals(cuerpo.get("apartados"),guardada.getApartadosReevaluacion());assertEquals("INCOMPLETA",guardada.getCategoriaReevaluacion());
    }
    @Test void cincoCamposCulturalesSeEditanMedianteInformacionSocial() throws Exception {
        Caso c=caso();c.n().setContextoCultural("Antecedente que debe conservarse íntegro");ninos.saveAndFlush(c.n());
        var ejemplos=json.readTree(getClass().getResourceAsStream("/static/escenarios-demo.json"));var cuerpo=json.convertValue(ejemplos.path("nino").get(0),new com.fasterxml.jackson.core.type.TypeReference<HashMap<String,Object>>(){});cuerpo.remove("contextoCultural");String ruta="/api/ninos/"+c.n().getCodigoCaso();
        var normalizar=req(ruta,HttpMethod.PUT,social,cuerpo);assertEquals(200,normalizar.getStatusCode().value(),normalizar.getBody());
        for(String k:List.of("idiomaComunicacion","costumbresTradiciones","vinculosCulturales","opinionCultural","recomendacionesCulturales"))cuerpo.put(k,"Contenido cultural local "+k+" á".repeat(1500));
        assertEquals(403,req(ruta,HttpMethod.PUT,psicologo,cuerpo).getStatusCode().value());var cambio=req(ruta,HttpMethod.PUT,social,cuerpo);assertEquals(200,cambio.getStatusCode().value(),cambio.getBody());
        var actual=datos(req(ruta,HttpMethod.GET,social,null));assertEquals("Antecedente que debe conservarse íntegro",actual.path("contextoCultural").asText());
        for(String k:List.of("idiomaComunicacion","costumbresTradiciones","vinculosCulturales","opinionCultural","recomendacionesCulturales"))assertEquals(cuerpo.get(k),actual.path(k).asText());
        var eventos=auditoria.findByCodigoCasoOrderByFechaAsc(c.n().getCodigoCaso());assertEquals(5,json.readTree(eventos.get(eventos.size()-1).getDescripcion()).path("cambios").size());int cantidad=eventos.size();assertEquals(200,req(ruta,HttpMethod.PUT,social,cuerpo).getStatusCode().value());assertEquals(cantidad,auditoria.findByCodigoCasoOrderByFechaAsc(c.n().getCodigoCaso()).size());
    }

}
