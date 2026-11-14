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

/** Fixtures sintéticas compartidas; cada suite exige habilitar una base aislada. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SoportePostgresIT {
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
        if(metodo==HttpMethod.POST && ruta.startsWith("/api/scoring/casos/") && ruta.endsWith("/decisiones") && cuerpo instanceof Map<?,?> m && !m.containsKey("versionPesos")){
            var copia=new LinkedHashMap<String,Object>();m.forEach((k,v)->copia.put(k.toString(),v));copia.put("versionPesos",datos(req("/api/scoring/pesos",HttpMethod.GET,comite,null)).path("version").asInt());cuerpo=copia;
        }
        HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);if(token!=null)h.setBearerAuth(token);
        return http.exchange(ruta,metodo,new HttpEntity<>(cuerpo,h),String.class);
    }
    JsonNode datos(ResponseEntity<String> r){try{return json.readTree(r.getBody());}catch(Exception e){throw new AssertionError(e);}}
    String login(String id){var r=req("/api/auth/login",HttpMethod.POST,null,Map.of("identificador",id,"password",CLAVE));assertEquals(200,r.getStatusCode().value(),r.getBody());return datos(r).path("token").asText();}
    ResponseEntity<String> aceptar(PerfilNino n,PerfilFamilia f){return req("/api/scoring/casos/"+n.getCodigoCaso()+"/decisiones",HttpMethod.POST,comite,Map.of("familia",f.getCodigoCaso(),"decision","ACEPTADA","motivo","Revisión sintética favorable para la prueba HU19"));}
    long asignar(Caso c){var r=aceptar(c.n,c.f);assertEquals(200,r.getStatusCode().value(),r.getBody());return vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(c.f.getRegistradoPor()).get(0).getId();}
    List<Integer> simultaneas(Callable<ResponseEntity<String>> a,Callable<ResponseEntity<String>> b) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch listos=new CountDownLatch(2),salida=new CountDownLatch(1);
        try{var fa=pool.submit(()->{listos.countDown();salida.await();return a.call().getStatusCode().value();});var fb=pool.submit(()->{listos.countDown();salida.await();return b.call().getStatusCode().value();});assertTrue(listos.await(5,TimeUnit.SECONDS));salida.countDown();return List.of(fa.get(20,TimeUnit.SECONDS),fb.get(20,TimeUnit.SECONDS)).stream().sorted().toList();}finally{pool.shutdownNow();}
    }
}
