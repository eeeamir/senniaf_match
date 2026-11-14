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
class HU19PostgresIT {
    @DynamicPropertySource static void configurar(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->System.getenv("HU19_TEST_DB_URL"));
        r.add("spring.datasource.username",()->System.getenv().getOrDefault("HU19_TEST_DB_USER","hu19_test"));r.add("spring.datasource.password",()->System.getenv().getOrDefault("HU19_TEST_DB_PASSWORD",""));
        r.add("spring.jpa.hibernate.ddl-auto",()->"validate");r.add("senniaf.demo.habilitado",()->false);
        r.add("senniaf.correo.habilitado",()->false);r.add("senniaf.seguridad.limite-auth",()->1000);
    }
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired PerfilNinoRepository ninos;
    @Autowired PerfilFamiliaRepository familias;
    @Autowired UsuarioRepository usuarios;
    @Autowired VinculacionRepository vinculaciones;
    @Autowired DecisionComiteRepository decisiones;
    @Autowired EventoCasoRepository auditoria;
    @Autowired ScoringService scoring;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    static final String CLAVE="ClaveSoloPruebas-HU19-2026";
    String comite,social,psicologo,otro;
    record Caso(PerfilNino n,PerfilFamilia f,String token){}
    @BeforeAll void cuentas(){
        usuario("HU19-CM","COMITE");usuario("HU19-TS","TRABAJADOR_SOCIAL");usuario("HU19-PS","PSICOLOGO");usuario("HU19-OTRO","TRABAJADOR_SOCIAL");
        comite=login("HU19-CM");social=login("HU19-TS");psicologo=login("HU19-PS");otro=login("HU19-OTRO");
    }
    void usuario(String id,String rol){
        if(usuarios.findByIdentificador(id).isPresent())return;
        Usuario u=new Usuario();u.setIdentificador(id);u.setRol(rol);u.setCorreo(id.toLowerCase(Locale.ROOT)+"@hu19.invalid");u.setNombre("Prueba");u.setApellido("HU19");u.setVerificado(true);u.setPasswordHash(encoder.encode(CLAVE));u.setFechaCreacion(LocalDateTime.now());usuarios.saveAndFlush(u);
    }
    Caso caso(){
        String clave="IT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);String actor="HU19-"+clave;
        usuario(actor,"FAMILIA");
        PerfilNino n=new PerfilNino();n.setCodigoCaso("N-"+clave);n.setEdad(8);n.setUnidadEdad(UnidadEdad.ANIOS);n.setPerfilCompleto(true);n.setTrabajadorSocialAsignado("HU19-TS");n.setPsicologoAsignado("HU19-PS");n.setRegistradoPor("HU19-TS");n.setFechaRegistro(LocalDateTime.now());n.setTieneHermanos(SiNo.NO);n.setCondicionesSalud(List.of(CondicionSalud.NINGUNA_CONOCIDA));n=ninos.saveAndFlush(n);
        PerfilFamilia f=new PerfilFamilia();f.setCodigoCaso("F-"+clave);f.setRegistradoPor(actor);f.setPerfilCompleto(true);f.setTipoFamilia(TipoFamilia.PAREJA);f.setEdadMinimaAceptada(1);f.setEdadMaximaAceptada(12);f.setRedApoyo(List.of(FuenteApoyo.PAREJA));f.setDisponibilidadViaje(DisponibilidadViaje.ALTA);f.setProvincia(ProvinciaPanama.PANAMA);f.setDisposicionHermanos(DisposicionHermanos.DISPUESTA);f.setFechaRegistro(LocalDateTime.now());f=familias.saveAndFlush(f);
        return new Caso(n,f,login(actor));
    }
    ResponseEntity<String> req(String ruta,HttpMethod metodo,String token,Object cuerpo){
        HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);if(token!=null)h.setBearerAuth(token);
        return http.exchange(ruta,metodo,new HttpEntity<>(cuerpo,h),String.class);
    }
    JsonNode datos(ResponseEntity<String> r){try{return json.readTree(r.getBody());}catch(Exception e){throw new AssertionError(e);}}
    String login(String id){var r=req("/api/auth/login",HttpMethod.POST,null,Map.of("identificador",id,"password",CLAVE));assertEquals(200,r.getStatusCode().value(),r.getBody());return datos(r).path("token").asText();}
    ResponseEntity<String> aceptar(PerfilNino n,PerfilFamilia f){return req("/api/scoring/casos/"+n.getCodigoCaso()+"/decisiones",HttpMethod.POST,comite,Map.of("familia",f.getCodigoCaso(),"decision","ACEPTADA","motivo","Revisión sintética favorable para la prueba HU19"));}
    long asignar(Caso c){var r=aceptar(c.n,c.f);assertEquals(200,r.getStatusCode().value(),r.getBody());return vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(c.f.getRegistradoPor()).get(0).getId();}
    @Test void respuestaPersisteYExpedientesSeConservan(){
        Caso c=caso();long totalN=ninos.count(),totalF=familias.count(),id=asignar(c);
        var r=req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token,Map.of("respuesta","ACEPTAR","estado","CERRADA","respondidoPor","OTRO"));assertEquals(200,r.getStatusCode().value(),r.getBody());
        assertEquals(totalN,ninos.count());assertEquals(totalF,familias.count());
        var guardada=vinculaciones.findById(id).orElseThrow();assertEquals(Vinculacion.Estado.RESPUESTA_ACEPTADA,guardada.getEstado());assertEquals(c.f.getRegistradoPor(),guardada.getRespondidoPor());assertTrue(guardada.isReservaVigente());
        var detalle=req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null);assertEquals(200,detalle.getStatusCode().value());assertEquals("RESPUESTA_ACEPTADA",datos(detalle).path("estado").asText());assertEquals(2,datos(detalle).path("historial").size());
        assertEquals(409,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token,Map.of("respuesta","RECHAZAR")).getStatusCode().value());
    }
    @Test void rechazoMantieneReservaSinPenalizarYNoExponeInfancia(){
        Caso c=caso();long id=asignar(c);var antes=scoring.candidato(c.n,c.f,scoring.pesos()).get("puntaje");
        assertEquals(403,req("/api/ninos/"+c.n.getCodigoCaso(),HttpMethod.GET,c.token,null).getStatusCode().value());
        assertEquals(200,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,c.token,Map.of("respuesta","RECHAZAR")).getStatusCode().value());
        for(String ruta:List.of("/api/ninos/"+c.n.getCodigoCaso(),"/api/ninos/"+c.n.getCodigoCaso()+"/auditoria","/api/vinculaciones/"+id,"/api/scoring/familias/"+c.f.getCodigoCaso()+"/expediente"))assertEquals(403,req(ruta,HttpMethod.GET,c.token,null).getStatusCode().value(),ruta);
        var propia=req("/api/vinculaciones/propias",HttpMethod.GET,c.token,null);assertFalse(propia.getBody().contains(c.n.getCodigoCaso()));assertFalse(propia.getBody().contains("motivo"));
        assertTrue(vinculaciones.findById(id).orElseThrow().isReservaVigente());assertFalse(familias.findById(c.f.getId()).orElseThrow().isReevaluacionPendiente());assertFalse(ninos.findById(c.n.getId()).orElseThrow().isBajaLogica());assertEquals(antes,scoring.candidato(c.n,familias.findById(c.f.getId()).orElseThrow(),scoring.pesos()).get("puntaje"));assertEquals(409,aceptar(c.n,c.f).getStatusCode().value());
    }
    @Test void permisosRealesYFamiliaAjena(){
        Caso c=caso(),otra=caso();long id=asignar(c);
        for(String token:List.of(comite,social,psicologo))assertEquals(200,req("/api/vinculaciones/"+id,HttpMethod.GET,token,null).getStatusCode().value());
        for(String token:List.of(otro,otra.token,c.token))assertEquals(403,req("/api/vinculaciones/"+id,HttpMethod.GET,token,null).getStatusCode().value());
        for(String token:List.of(comite,social,psicologo,otra.token))assertEquals(403,req("/api/vinculaciones/"+id+"/respuesta",HttpMethod.POST,token,Map.of("respuesta","ACEPTAR")).getStatusCode().value());
        assertEquals(403,req("/api/vinculaciones",HttpMethod.GET,c.token,null).getStatusCode().value());assertEquals(401,req("/api/vinculaciones",HttpMethod.GET,null,null).getStatusCode().value());
        HttpHeaders h=new HttpHeaders();h.setBearerAuth(c.token);h.set("X-User-Role","COMITE");h.set("X-User-Id","HU19-CM");assertEquals(403,http.exchange("/api/vinculaciones",HttpMethod.GET,new HttpEntity<>(h),String.class).getStatusCode().value());
    }
    List<Integer> simultaneas(Callable<ResponseEntity<String>> a,Callable<ResponseEntity<String>> b) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch listos=new CountDownLatch(2),salida=new CountDownLatch(1);
        try{var fa=pool.submit(()->{listos.countDown();salida.await();return a.call().getStatusCode().value();});var fb=pool.submit(()->{listos.countDown();salida.await();return b.call().getStatusCode().value();});assertTrue(listos.await(5,TimeUnit.SECONDS));salida.countDown();return List.of(fa.get(20,TimeUnit.SECONDS),fb.get(20,TimeUnit.SECONDS)).stream().sorted().toList();}finally{pool.shutdownNow();}
    }
    @Test void dosFamiliasNoReservanElMismoNino() throws Exception {Caso a=caso(),b=caso();assertEquals(List.of(200,409),simultaneas(()->aceptar(a.n,a.f),()->aceptar(a.n,b.f)));assertEquals(1,decisiones.findByCodigoNinoOrderByFechaAsc(a.n.getCodigoCaso()).size());}
    @Test void dosNinosNoReservanLaMismaFamilia() throws Exception {Caso a=caso(),b=caso();assertEquals(List.of(200,409),simultaneas(()->aceptar(a.n,a.f),()->aceptar(b.n,a.f)));assertEquals(1,vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(a.f.getRegistradoPor()).size());}
    @Test void dobleAceptacionNoDuplicaVinculacion() throws Exception {Caso c=caso();assertEquals(List.of(200,409),simultaneas(()->aceptar(c.n,c.f),()->aceptar(c.n,c.f)));assertEquals(1,vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(c.f.getRegistradoPor()).size());}
    @Test void respuestasContradictoriasSeSerializan() throws Exception {Caso c=caso();long id=asignar(c);String ruta="/api/vinculaciones/"+id+"/respuesta";assertEquals(List.of(200,409),simultaneas(()->req(ruta,HttpMethod.POST,c.token,Map.of("respuesta","ACEPTAR")),()->req(ruta,HttpMethod.POST,c.token,Map.of("respuesta","RECHAZAR"))));assertEquals(2,datos(req("/api/vinculaciones/"+id,HttpMethod.GET,comite,null)).path("historial").size());}
    @Test void disponibilidadDependeDeReservaNoDeAceptacionHistorica(){
        Caso c=caso();long id=asignar(c);
        assertEquals(true,scoring.caso(c.n.getCodigoCaso()).get("propuestaAceptada"));
        // Simula exclusivamente en esta base desechable una liberación institucional futura.
        jdbc.update("update vinculaciones set reserva_vigente=false where id=?",id);
        var actual=scoring.caso(c.n.getCodigoCaso());
        assertEquals(false,actual.get("propuestaAceptada"));
        assertTrue(((List<?>)actual.get("familias")).stream().anyMatch(f->c.f.getCodigoCaso().equals(((Map<?,?>)f).get("codigo"))));
        assertTrue(decisiones.existsByCodigoNinoAndDecision(c.n.getCodigoCaso(),"ACEPTADA"));
    }

}
