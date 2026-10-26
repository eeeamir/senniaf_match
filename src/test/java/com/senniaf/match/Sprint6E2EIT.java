package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.repository.PerfilNinoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas HTTP reales contra Spring Boot y PostgreSQL; ejecutar mvn verify -Pe2e. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Sprint6E2EIT {
    @LocalServerPort int port;
    @Autowired TestRestTemplate cliente;
    @Autowired PerfilNinoRepository repoNinos;
    @Autowired ObjectMapper mapper;
    @Value("${SENNIAF_DEMO_PASSWORD:Cambio123!}") String passwordDemo;

    private String comite, profesional, psicologo, familia, cedula, nino, familiaCaso;
    private final Map<String,Object> entrevista = new LinkedHashMap<>();
    private final Map<String,Object> perfilInfantil = new LinkedHashMap<>();

    @BeforeAll
    void preparar() {
        comite = login("CM-001"); profesional = login("TS-001"); psicologo = login("PS-014");
        entrevista.put("tipoFamilia", "PAREJA"); entrevista.put("redApoyo", List.of("FAMILIA_EXTENDIDA"));
        entrevista.put("provincia", "PANAMA"); entrevista.put("disponibilidadViaje", "ALTA");
        entrevista.put("edadMinimaAceptada", 3); entrevista.put("edadMaximaAceptada", 12);
        entrevista.put("disposicionHermanos", "DISPUESTA");
        entrevista.put("condicionesPuedeAcompanarHoy", List.of("ASMA"));
        perfilInfantil.put("edad", 7); perfilInfantil.put("nivelEscolar", "PRIMARIA");
        perfilInfantil.put("rendimientoAcademico", "BUENO");
        perfilInfantil.put("habilidades", List.of("MATEMATICAS"));
        perfilInfantil.put("gustos", List.of("DEPORTES"));
        perfilInfantil.put("personalidad", List.of("SOCIABLE"));
        perfilInfantil.put("alergias", List.of("NINGUNA_CONOCIDA"));
        perfilInfantil.put("intoleranciaLactosa", "NO");
        perfilInfantil.put("condicionesSalud", List.of("ASMA"));
        perfilInfantil.put("necesidadesApoyo", "Acompañamiento de salud y escuela");
        perfilInfantil.put("tieneHermanos", "SI"); perfilInfantil.put("cantidadHermanos", 1);
    }

    private ResponseEntity<String> peticion(String ruta, HttpMethod metodo, String token, Object cuerpo, Map<String,String> extra) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        extra.forEach(headers::set);
        return cliente.exchange("http://localhost:" + port + ruta, metodo, new HttpEntity<>(cuerpo, headers), String.class);
    }
    private ResponseEntity<String> req(String ruta, HttpMethod metodo, String token, Object cuerpo) {
        return peticion(ruta, metodo, token, cuerpo, Map.of());
    }
    private JsonNode json(ResponseEntity<String> r) {
        try { return mapper.readTree(r.getBody()); } catch(Exception e) { throw new AssertionError(e); }
    }
    private String login(String id) {
        ResponseEntity<String> r = req("/api/auth/login", HttpMethod.POST, null,
                Map.of("identificador", id, "password", passwordDemo));
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        return json(r).path("token").asText();
    }

    @Test @Order(1)
    void e2e01RegistroVerificacionYEntrevistaEnPostgreSQL() {
        String secuencia = Long.toString(System.currentTimeMillis() % 900000 + 100000);
        cedula = "8-456-" + secuencia;
        String correo = "familia" + secuencia + "@sprint6.demo";
        ResponseEntity<String> alta = req("/api/auth/registro", HttpMethod.POST, null,
                Map.of("nombre","Familia", "apellido","Prueba", "cedula",cedula,
                       "correo",correo, "password","Prueba12345!"));
        assertEquals(201, alta.getStatusCode().value(), alta.getBody());
        String codigo = json(alta).path("codigoDemo").asText();
        ResponseEntity<String> verificado = req("/api/auth/verificar", HttpMethod.POST, null,
                Map.of("cedula",cedula, "codigo",codigo));
        assertEquals(200, verificado.getStatusCode().value(), verificado.getBody());
        familia = json(verificado).path("token").asText();
        ResponseEntity<String> creado = req("/api/familias", HttpMethod.POST, familia, entrevista);
        assertEquals(201, creado.getStatusCode().value(), creado.getBody());
        familiaCaso = json(creado).path("codigoCaso").asText();
        assertTrue(json(req("/api/familias/mi-perfil", HttpMethod.GET, familia, null)).path("perfilCompleto").asBoolean());
    }

    @Test @Order(2)
    void e2e02RegistroEdicionYAuditoriaDeCasoPropio() {
        ResponseEntity<String> creado = req("/api/ninos", HttpMethod.POST, profesional, perfilInfantil);
        assertEquals(201, creado.getStatusCode().value(), creado.getBody());
        nino = json(creado).path("codigoCaso").asText();
        assertTrue(json(req("/api/ninos", HttpMethod.GET, profesional, null)).toString().contains(nino));
        Map<String,Object> editado = new LinkedHashMap<>(perfilInfantil);
        editado.put("necesidadesApoyo", "Necesita acompañamiento escolar y seguimiento respiratorio");
        assertEquals(200, req("/api/ninos/" + nino, HttpMethod.PUT, profesional, editado).getStatusCode().value());
        assertEquals(2, json(req("/api/ninos/" + nino + "/auditoria", HttpMethod.GET, profesional, null)).size());
    }

    @Test @Order(3)
    void e2e03RankingReproducibleYExplicable() {
        ResponseEntity<String> primera = req("/api/scoring/casos", HttpMethod.GET, comite, null);
        ResponseEntity<String> segunda = req("/api/scoring/casos", HttpMethod.GET, comite, null);
        assertEquals(200, primera.getStatusCode().value());
        assertEquals(primera.getBody(), segunda.getBody(), "El mismo estado produce el mismo ranking");
        String cuerpo = primera.getBody();
        assertTrue(cuerpo.contains(nino)); assertTrue(cuerpo.contains(familiaCaso));
        assertTrue(cuerpo.contains("logistica") && cuerpo.contains("hermanos") && cuerpo.contains("salud"));
    }

    @Test @Order(4)
    void e2e04PerfilIncompletoQuedaExcluido() {
        PerfilNino incompleto = new PerfilNino();
        String codigo = "INCOMPLETO-" + UUID.randomUUID().toString().substring(0, 8);
        incompleto.setCodigoCaso(codigo); incompleto.setPerfilCompleto(false);
        incompleto.setRegistradoPor("TS-001"); repoNinos.save(incompleto);
        String ranking = req("/api/scoring/casos", HttpMethod.GET, comite, null).getBody();
        assertFalse(ranking.contains(codigo));
    }

    @Test @Order(5)
    void e2e05TresReevaluacionesSinSancionAutomatica() {
        for (int i = 1; i <= 3; i++) {
            ResponseEntity<String> accion = req("/api/scoring/casos/" + nino + "/decisiones", HttpMethod.POST, comite,
                    Map.of("familia", familiaCaso,"decision","REEVALUACION",
                           "motivo","Solicitar actualización de información familiar número " + i));
            assertEquals(200, accion.getStatusCode().value(), accion.getBody());
            JsonNode estado = json(req("/api/familias/mi-perfil", HttpMethod.GET, familia, null));
            assertEquals(i - 1, estado.path("numeroReevaluaciones").asInt(),
                    "El Comité solicita la corrección, pero todavía no cuenta como completada");
            assertTrue(estado.path("reevaluacionPendiente").asBoolean());
            // Consultar el formulario o regresar a seguimiento no cambia el contador.
            assertEquals(200, req("/api/familias/mi-perfil/detalle", HttpMethod.GET, familia, null).getStatusCode().value());
            assertEquals(i - 1, json(req("/api/familias/mi-perfil", HttpMethod.GET, familia, null))
                    .path("numeroReevaluaciones").asInt());
            assertFalse(req("/api/scoring/casos/" + nino, HttpMethod.GET, comite, null).getBody().contains(familiaCaso));
            assertEquals(200, req("/api/familias/mi-perfil", HttpMethod.PUT, familia, entrevista).getStatusCode().value());
            JsonNode corregido = json(req("/api/familias/mi-perfil", HttpMethod.GET, familia, null));
            assertEquals(i, corregido.path("numeroReevaluaciones").asInt());
            assertFalse(corregido.path("reevaluacionPendiente").asBoolean());
            // Volver a guardar sin otra solicitud no suma una segunda vez.
            assertEquals(200, req("/api/familias/mi-perfil", HttpMethod.PUT, familia, entrevista).getStatusCode().value());
            assertEquals(i, json(req("/api/familias/mi-perfil", HttpMethod.GET, familia, null))
                    .path("numeroReevaluaciones").asInt());
        }
        JsonNode estado = json(req("/api/familias/mi-perfil", HttpMethod.GET, familia, null));
        assertTrue(estado.path("requiereRevisionTecnica").asBoolean());
        assertFalse(estado.path("reevaluacionPendiente").asBoolean());
        assertTrue(req("/api/scoring/casos/" + nino, HttpMethod.GET, comite, null).getBody().contains(familiaCaso));
    }

    @Test @Order(6)
    void e2e06CincoControlesDeSeguridad() {
        assertEquals(401, req("/api/scoring/casos", HttpMethod.GET, null, null).getStatusCode().value());
        assertEquals(403, req("/api/scoring/casos", HttpMethod.GET, familia, null).getStatusCode().value());
        assertEquals(403, peticion("/api/scoring/casos", HttpMethod.GET, profesional, null,
                Map.of("X-User-Role","COMITE", "X-User-Id","CM-001")).getStatusCode().value());
        assertEquals(401, req("/api/scoring/casos", HttpMethod.GET,
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", null).getStatusCode().value());
        assertEquals(403, req("/api/ninos/" + nino, HttpMethod.GET, psicologo, null).getStatusCode().value());
    }

    @Test @Order(7)
    void e2e07PesosInvalidosNoAlteranConfiguracion() {
        JsonNode antes = json(req("/api/scoring/pesos", HttpMethod.GET, comite, null));
        ResponseEntity<String> mal = req("/api/scoring/pesos", HttpMethod.PUT, comite,
                Map.of("edad", 20, "hermanos", 25, "salud", 35, "apoyo", 15, "logistica", 6));
        assertEquals(400, mal.getStatusCode().value());
        JsonNode despues = json(req("/api/scoring/pesos", HttpMethod.GET, comite, null));
        assertEquals(antes.path("version").asInt(), despues.path("version").asInt());
    }

    @Test @Order(8)
    void e2e08MedicionLocalDeLatenciaDelRanking() {
        List<Long> tiempos = new ArrayList<>();
        IntStream.range(0, 15).forEach(i -> {
            long inicio = System.nanoTime();
            ResponseEntity<String> r = req("/api/scoring/casos", HttpMethod.GET, comite, null);
            assertEquals(200, r.getStatusCode().value());
            tiempos.add((System.nanoTime() - inicio) / 1_000_000);
        });
        Collections.sort(tiempos);
        System.out.println("SPRINT6_LATENCIA_RANKING_MS p50=" + tiempos.get(7)
                + " p95=" + tiempos.get(14) + " n=15 entorno=CI/datos_sinteticos");
    }
}
