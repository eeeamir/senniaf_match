package com.senniaf.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="HU19_TEST_DB_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z_]+")
class CierreInstitucionalPostgresIT extends SoportePostgresIT {
    @org.springframework.beans.factory.annotation.Autowired com.senniaf.match.service.VinculacionService servicioVinculaciones;
    Map<String,Object> cierre(){return Map.of("motivo","Cierre sintético confirmado por actuación institucional para esta prueba","confirmacionAutoridad",true);}
    void cerrar(long id){var r=req("/api/vinculaciones/"+id+"/cierre",HttpMethod.POST,comite,cierre());assertEquals(200,r.getStatusCode().value(),r.getBody());assertEquals("CERRADA",datos(r).path("estado").asText());}
    org.springframework.http.ResponseEntity<String> liberar(long id,String quien){return req("/api/vinculaciones/"+id+"/disponibilidad",HttpMethod.POST,comite,Map.of("expediente",quien,"justificacion","Revisión sintética individual de disponibilidad y elegibilidad institucional"));}
    @Test void cierreConservaInterrupcionYReservasHastaRevisionSeparada(){
        Caso c=caso(),otra=caso();long id=asignar(c);req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR"));
        autorizarInicio(id);
        var informe=Map.of("motivo","ADAPTACION","justificacion","Interrupción sintética cuyo antecedente debe permanecer después del cierre");
        assertEquals(200,req("/api/vinculaciones/"+id+"/interrupcion",HttpMethod.POST,social,informe).getStatusCode().value());
        var puntaje=scoring.candidato(c.n(),c.f(),scoring.pesos()).get("puntaje");cerrar(id);
        assertTrue(vinculaciones.existsByNinoCodigoCasoAndReservaVigenteTrue(c.n().getCodigoCaso()));assertTrue(vinculaciones.existsByFamiliaCodigoCasoAndReservaVigenteTrue(c.f().getCodigoCaso()));assertEquals(409,aceptar(c.n(),otra.f()).getStatusCode().value());
        var d=datos(req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null));assertEquals(informe.get("justificacion"),d.path("interrupcion").path("justificacion").asText());assertEquals("HU19-CM",d.path("cierre").path("registradoPor").asText());assertFalse(d.path("cierre").path("registradoEl").isNull());
        assertEquals(200,liberar(id,"NINO").getStatusCode().value());assertFalse(vinculaciones.existsByNinoCodigoCasoAndReservaVigenteTrue(c.n().getCodigoCaso()));assertTrue(vinculaciones.existsByFamiliaCodigoCasoAndReservaVigenteTrue(c.f().getCodigoCaso()));
        assertEquals(200,aceptar(c.n(),otra.f()).getStatusCode().value());assertEquals(200,liberar(id,"FAMILIA").getStatusCode().value());assertFalse(vinculaciones.findById(id).orElseThrow().isReservaVigente());assertEquals(409,liberar(id,"NINO").getStatusCode().value());
        assertEquals(puntaje,scoring.candidato(c.n(),familias.findById(c.f().getId()).orElseThrow(),scoring.pesos()).get("puntaje"));
        var propia=req("/api/vinculaciones/propias",HttpMethod.GET,c.token(),null);assertFalse(propia.getBody().contains(c.n().getCodigoCaso()));assertFalse(propia.getBody().contains("ACT-CIERRE"));assertEquals(403,req("/api/ninos/"+c.n().getCodigoCaso(),HttpMethod.GET,c.token(),null).getStatusCode().value());
    }
    @Test void familiaVuelveAntesQueNinoSinRepetirParejaInterrumpidaNiEludirNuevaReserva(){
        Caso c=caso(),otra=caso(),tercera=caso();long id=asignar(c);
        assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        autorizarInicio(id);
        assertEquals(200,req("/api/vinculaciones/"+id+"/interrupcion",HttpMethod.POST,social,Map.of("motivo","ADAPTACION","justificacion","Interrupción sintética para comprobar la reincorporación independiente")).getStatusCode().value());
        cerrar(id);
        assertEquals(200,liberar(id,"FAMILIA").getStatusCode().value());
        assertTrue(vinculaciones.findById(id).orElseThrow().isReservaVigente());
        assertTrue(vinculaciones.existsByNinoCodigoCasoAndReservaVigenteTrue(c.n().getCodigoCaso()));
        assertFalse(vinculaciones.existsByFamiliaCodigoCasoAndReservaVigenteTrue(c.f().getCodigoCaso()));
        var ranking=datos(req("/api/scoring/casos",HttpMethod.GET,comite,null));
        assertFalse(java.util.stream.StreamSupport.stream(ranking.spliterator(),false).anyMatch(n->n.path("codigo").asText().equals(c.n().getCodigoCaso())));
        var candidato=java.util.stream.StreamSupport.stream(ranking.spliterator(),false).filter(n->n.path("codigo").asText().equals(otra.n().getCodigoCaso())).findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(candidato.path("familias").spliterator(),false).anyMatch(f->f.path("codigo").asText().equals(c.f().getCodigoCaso())));
        assertEquals(200,aceptar(otra.n(),c.f()).getStatusCode().value());
        assertEquals(200,liberar(id,"NINO").getStatusCode().value());
        assertTrue(vinculaciones.existsByFamiliaCodigoCasoAndReservaVigenteTrue(c.f().getCodigoCaso()));
        assertEquals(409,aceptar(tercera.n(),c.f()).getStatusCode().value());
        assertEquals(409,aceptar(c.n(),c.f()).getStatusCode().value());
        var casoLiberado=datos(req("/api/scoring/casos/"+c.n().getCodigoCaso(),HttpMethod.GET,comite,null));
        assertTrue(casoLiberado.path("familiasRechazadas").toString().contains(c.f().getCodigoCaso()));
        assertFalse(java.util.stream.StreamSupport.stream(casoLiberado.path("familias").spliterator(),false).anyMatch(f->f.path("codigo").asText().equals(c.f().getCodigoCaso())));
        assertEquals(200,aceptar(c.n(),otra.f()).getStatusCode().value());
        assertEquals(409,aceptar(c.n(),tercera.f()).getStatusCode().value());
        assertEquals(2,auditoria.findByCodigoCasoOrderByFechaAsc(c.n().getCodigoCaso()).stream().filter(e->"DISPONIBILIDAD_REVISADA".equals(e.getAccion())).count());
    }

    @Test void cierreDePropuestaSinRespuestaNoInventaAceptacion(){Caso c=caso();long id=asignar(c);cerrar(id);var v=vinculaciones.findById(id).orElseThrow();assertNull(v.getFechaRespuesta());assertNull(v.getRespondidoPor());assertTrue(v.isReservaVigente());assertEquals(409,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token(),Map.of("respuesta","ACEPTAR")).getStatusCode().value());}
    @Test void cierreRequiereConfirmacionDatosValidosYRol(){
        Caso c=caso();long id=asignar(c);String ruta="/api/vinculaciones/"+id+"/cierre";
        for(String token:List.of(c.token(),social,psicologo,otro))assertEquals(403,req(ruta,HttpMethod.POST,token,cierre()).getStatusCode().value());
        for(var cambio:List.of(Map.<String,Object>of("confirmacionAutoridad",false),Map.<String,Object>of("motivo","corto"),Map.<String,Object>of("motivo","a".repeat(2001)))){var m=new HashMap<>(cierre());m.putAll(cambio);assertEquals(400,req(ruta,HttpMethod.POST,comite,m).getStatusCode().value());}
        assertNull(vinculaciones.findById(id).orElseThrow().getFechaCierre());assertEquals(409,liberar(id,"NINO").getStatusCode().value());
    }
    @Test void reservaNoSeLiberaSiExpedienteNoEsElegible(){
        Caso c=caso();long id=asignar(c);cerrar(id);var n=ninos.findById(c.n().getId()).orElseThrow();n.setCierrePorAdopcion(true);ninos.saveAndFlush(n);var f=familias.findById(c.f().getId()).orElseThrow();f.setReevaluacionPendiente(true);familias.saveAndFlush(f);
        assertEquals(409,liberar(id,"NINO").getStatusCode().value());assertEquals(409,liberar(id,"FAMILIA").getStatusCode().value());
        for(String token:List.of(c.token(),social,psicologo))assertEquals(403,req("/api/vinculaciones/"+id+"/disponibilidad",HttpMethod.POST,token,Map.of("expediente","NINO","justificacion","No autorizado para modificar esta reserva")).getStatusCode().value());
        assertTrue(vinculaciones.findById(id).orElseThrow().isReservaVigente());
    }
    @Test void cierresSimultaneosNoSobrescribenActuacion() throws Exception {Caso c=caso();long id=asignar(c);String ruta="/api/vinculaciones/"+id+"/cierre";assertEquals(List.of(200,409),simultaneas(()->req(ruta,HttpMethod.POST,comite,cierre()),()->req(ruta,HttpMethod.POST,comite,cierre())));assertEquals(1,auditoria.findByCodigoCasoOrderByFechaAsc(c.n().getCodigoCaso()).stream().filter(e->"CIERRE_INSTITUCIONAL".equals(e.getAccion())).count());}
    @Test void cierreRegistraFechaServidorSinAceptarFechaManual(){
        var anterior=TimeZone.getDefault();
        try{
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Caso c=caso();
            var fechaLocal=LocalDate.now(ZoneId.of("America/Panama")).minusDays(1);
            var decision=new com.senniaf.match.model.DecisionComite();decision.setCodigoNino(c.n().getCodigoCaso());decision.setCodigoFamilia(c.f().getCodigoCaso());decision.setDecision("ACEPTADA");decision.setFuncionario("HU19-CM");decision.setMotivo("Antecedente sintético nocturno para validar la fecha institucional");
            decision.setFecha(fechaLocal.atTime(21,0).atZone(ZoneId.of("America/Panama")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());decision=decisiones.saveAndFlush(decision);servicioVinculaciones.crear(decision,c.n(),c.f());long id=vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(c.f().getRegistradoPor()).get(0).getId();
            var cuerpo=new HashMap<>(cierre());cuerpo.put("fechaCierre",fechaLocal.toString());
            var r=req("/api/vinculaciones/"+id+"/cierre",HttpMethod.POST,comite,cuerpo);assertEquals(200,r.getStatusCode().value(),r.getBody());assertEquals(LocalDate.now(ZoneId.of("America/Panama")).toString(),datos(r).path("cierre").path("fecha").asText());assertTrue(datos(r).path("cierre").path("referencia").isNull());assertTrue(datos(r).path("cierre").path("autoridad").isNull());
        }finally{TimeZone.setDefault(anterior);}
    }

}
