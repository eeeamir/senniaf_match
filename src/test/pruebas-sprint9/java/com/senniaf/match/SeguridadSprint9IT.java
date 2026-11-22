package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seguridad del Sprint 9: control de acceso y autorización por rol, validación de entradas
 * (inyección SQL y XSS), y que los errores no expongan detalles internos. Casos SE-01 a SE-40.
 * Las pruebas de seguridad forman parte del alcance del Sprint 9.
 */
@EnabledIfEnvironmentVariable(named = "HU19_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z0-9_]+")
class SeguridadSprint9IT extends Sprint9Base {

    static final String[] PAYLOADS_SQL_LOGIN = {"' OR '1'='1", "admin'--", "1; DROP TABLE usuarios"};
    static final String[] PAYLOADS_XSS = {"<script>alert('x')</script>", "<img src=x onerror=alert(1)>"};

    /** (caso, rol que intenta, acción que no le corresponde). */
    static Stream<Arguments> accionesNoAutorizadas() {
        return Stream.of(
                Arguments.of("SE-01", "FAMILIA", "ACEPTAR"),
                Arguments.of("SE-02", "FAMILIA", "RECHAZAR"),
                Arguments.of("SE-03", "FAMILIA", "PROPONER"),
                Arguments.of("SE-04", "FAMILIA", "REALIZADA"),
                Arguments.of("SE-05", "PSICOLOGO", "ACEPTAR"),
                Arguments.of("SE-06", "PSICOLOGO", "RECHAZAR"),
                Arguments.of("SE-07", "PSICOLOGO", "PROPONER"),
                Arguments.of("SE-08", "PSICOLOGO", "REALIZADA"),
                Arguments.of("SE-09", "PSICOLOGO", "ACEPTAR_PROPUESTA"),
                Arguments.of("SE-10", "COMITE", "ACEPTAR"),
                Arguments.of("SE-11", "COMITE", "RECHAZAR"),
                Arguments.of("SE-12", "COMITE", "PROPONER"),
                Arguments.of("SE-13", "COMITE", "REALIZADA"),
                Arguments.of("SE-14", "TRABAJADOR_SOCIAL", "ACEPTAR_PROPUESTA"),
                Arguments.of("SE-15", "TRABAJADOR_SOCIAL", "OTRO_HORARIO")
        );
    }

    String tokenDe(String rol, Caso c) {
        return switch (rol) {
            case "FAMILIA" -> c.token();
            case "PSICOLOGO" -> psicologo;
            case "COMITE" -> comite;
            case "TRABAJADOR_SOCIAL" -> social;
            default -> throw new IllegalArgumentException(rol);
        };
    }

    @ParameterizedTest(name = "{0} {1} no puede ejecutar la acción {2}")
    @MethodSource("accionesNoAutorizadas")
    void SE01_a_SE15_rolNoAutorizadoNoEjecutaLaAccion(String id, String rol, String accion) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        long version = versionCita(cita, social);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("accion", accion);
        cuerpo.put("version", version);
        if (accion.equals("RECHAZAR")) cuerpo.put("motivo", "NO_ATENDER");
        if (accion.equals("PROPONER") || accion.equals("OTRO_HORARIO")) {
            String[] h = primerHueco(vin, c);
            cuerpo.put("fecha", h[0]);
            cuerpo.put("hora", h[1]);
            if (accion.equals("PROPONER")) cuerpo.put("motivo", "AJUSTE_AGENDA");
        }
        var r = req("/api/citas/" + cita + "/acciones", HttpMethod.POST, tokenDe(rol, c), cuerpo);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()),
                rol + " no debe poder " + accion + "; respondió " + r.getStatusCode().value());
        assertEquals("PENDIENTE", estadoCita(cita, social), "La cita no debe cambiar tras el intento no autorizado");
    }

    // ---------- Acceso sin sesión ----------

    @ParameterizedTest(name = "SE-16-{index} sin sesión: {0} {1}")
    @ValueSource(strings = {"GET /api/citas", "GET /api/citas/opciones", "GET /api/vinculaciones/propias",
            "GET /api/scoring/casos", "GET /api/familias/mi-perfil", "POST /api/citas"})
    void SE16_endpointsProtegidosRechazanPeticionesSinSesion(String entrada) {
        String[] partes = entrada.split(" ", 2);
        var metodo = HttpMethod.valueOf(partes[0]);
        var r = req(partes[1], metodo, null, metodo == HttpMethod.POST ? Map.of() : null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), entrada + " respondió " + r.getStatusCode().value());
    }

    @Test
    void SE17_familiaNoAccedeAlAnalisisDelComite() {
        var c = caso();
        assertTrue(esErrorDeCliente(req("/api/scoring/casos", HttpMethod.GET, c.token(), null).getStatusCode().value()));
    }

    @Test
    void SE18_encabezadoDeRolNoEleveLosPermisos() {
        var c = caso();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(c.token());
        h.set("X-User-Role", "COMITE");
        ResponseEntity<String> r = http.exchange("/api/scoring/casos", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()),
                "Un encabezado X-User-Role enviado por el cliente no debe otorgar permisos del Comité");
    }

    @Test
    void SE19_familiaNoVeExpedienteInfantilEnSusVinculaciones() {
        var c = caso();
        vinculacionAceptada(c);
        var propias = datos(req("/api/vinculaciones/propias", HttpMethod.GET, c.token(), null));
        assertFalse(propias.get(0).has("expedienteInfantil"), "La familia no debe recibir el expediente infantil");
    }

    @Test
    void SE20_familiaNoVeLaVinculacionDeOtraFamilia() {
        var propia = caso();
        var ajena = caso();
        long vinAjena = vinculacionAceptada(ajena);
        assertTrue(esErrorDeCliente(req("/api/vinculaciones/" + vinAjena, HttpMethod.GET, propia.token(), null)
                .getStatusCode().value()));
    }

    @Test
    void SE21_otroTrabajadorSocialNoRespondeLaCitaAjena() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        long version = versionCita(cita, social);
        var r = req("/api/citas/" + cita + "/acciones", HttpMethod.POST, otro,
                Map.of("accion", "ACEPTAR", "version", version));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
        assertEquals("PENDIENTE", estadoCita(cita, social));
    }

    @Test
    void SE22_respuestasNoExponenHashDeClave() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertFalse(req("/api/citas/" + cita, HttpMethod.GET, c.token(), null).getBody().contains("passwordHash"));
        assertFalse(req("/api/vinculaciones/propias", HttpMethod.GET, c.token(), null).getBody().contains("passwordHash"));
    }

    // ---------- Inyección SQL ----------

    @ParameterizedTest(name = "SE-23-{index} inyección SQL en el inicio de sesión: [{0}]")
    @ValueSource(strings = {"' OR '1'='1", "admin'--", "1; DROP TABLE usuarios"})
    void SE23_inyeccionSqlEnLoginNoAccedeNiRompe(String identificador) {
        var r = req("/api/auth/login", HttpMethod.POST, null, Map.of("identificador", identificador, "password", CLAVE));
        assertNotEquals(200, r.getStatusCode().value(), "La inyección no debe iniciar sesión");
        assertTrue(r.getStatusCode().value() < 500, "La inyección no debe producir un error interno");
    }

    @Test
    void SE24_inyeccionSqlEnMotivoDeCitaSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "' OR 1=1 --", "fecha", h[0], "hora", h[1]));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @ParameterizedTest(name = "SE-25-{index} inyección SQL en el mensaje de la cita: [{0}]")
    @ValueSource(strings = {"'); DROP TABLE citas;--", "x' UNION SELECT clave FROM usuarios --"})
    void SE25_inyeccionSqlEnMensajeNoAfectaLaBaseDeDatos(String mensaje) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", h[0], "hora", h[1], "mensaje", mensaje));
        assertTrue(r.getStatusCode().value() < 500, "El mensaje malicioso no debe producir un error interno");
        assertEquals(200, req("/api/citas", HttpMethod.GET, social, null).getStatusCode().value(),
                "La tabla de citas debe seguir disponible");
    }

    // ---------- XSS ----------

    @ParameterizedTest(name = "SE-26-{index} XSS en la respuesta del rechazo: [{0}]")
    @ValueSource(strings = {"<script>alert('x')</script>", "<img src=x onerror=alert(1)>"})
    void SE26_xssEnMensajeSeDevuelveComoDatoJson(String payload) throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        var r = accionCita(cita, social, "RECHAZAR", Map.of("motivo", "NO_ATENDER", "mensaje", payload));
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        assertTrue(r.getHeaders().getContentType() != null
                && r.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON),
                "La respuesta debe ser JSON, no HTML ejecutable");
        JsonNode cuerpo = json.readTree(r.getBody());
        assertNotNull(cuerpo);
    }

    @ParameterizedTest(name = "SE-27-{index} XSS en el mensaje de solicitud: [{0}]")
    @ValueSource(strings = {"<script>alert('x')</script>", "<img src=x onerror=alert(1)>"})
    void SE27_xssEnSolicitudSeGuardaComoTextoYNoComoHtml(String payload) throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", h[0], "hora", h[1], "mensaje", payload));
        if (r.getStatusCode().is2xxSuccessful()) {
            assertTrue(r.getHeaders().getContentType() != null
                    && r.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON));
            json.readTree(r.getBody());
        } else {
            assertTrue(esErrorDeCliente(r.getStatusCode().value()));
        }
    }

    // ---------- Errores sin detalles internos ----------

    static Stream<String> cuerposMalformados() {
        return Stream.of("{\"motivo\":", "[]", "{\"vinculacionId\":\"x\",\"fecha\":{}}");
    }

    @ParameterizedTest(name = "SE-28-{index} cuerpo malformado no expone detalles internos: [{0}]")
    @MethodSource("cuerposMalformados")
    void SE28_erroresNoExponenDetallesInternos(String cuerpo) {
        var c = caso();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(c.token());
        ResponseEntity<String> r = http.exchange("/api/citas", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
        String texto = r.getBody() == null ? "" : r.getBody();
        for (String filtrado : new String[]{"Exception", "at com.senniaf", "org.hibernate", "org.postgresql", "SQLState", "stackTrace"})
            assertFalse(texto.contains(filtrado), "El error expone el detalle interno: " + filtrado);
    }

    @Test
    void SE29_loginNoRevelaSiElUsuarioExiste() {
        var existente = req("/api/auth/login", HttpMethod.POST, null, Map.of("identificador", "HU19-CM", "password", "clave-incorrecta-prueba"));
        var inexistente = req("/api/auth/login", HttpMethod.POST, null, Map.of("identificador", "NO-EXISTE-SE29", "password", "clave-incorrecta-prueba"));
        assertEquals(existente.getStatusCode().value(), inexistente.getStatusCode().value(),
                "Un usuario existente y uno inexistente deben recibir la misma respuesta");
    }

    @Test
    void SE30_errorDeAutorizacionNoExponeLaCitaAjena() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        var r = req("/api/citas/" + cita, HttpMethod.GET, otro, null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
        assertFalse(r.getBody() != null && r.getBody().contains(c.n().getCodigoCaso()),
                "El error no debe revelar el código del caso ajeno");
    }

    @ParameterizedTest(name = "SE-31-{index} token alterado rechazado: {0}")
    @ValueSource(strings = {"token-falso-sprint9", "eyJhbGciOiJub25lIn0.e30."})
    void SE31_tokenAlteradoSeRechaza(String token) {
        var r = req("/api/citas", HttpMethod.GET, token, null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Código " + r.getStatusCode().value());
    }

    @Test
    void SE32_loginConClaveVaciaSeRechaza() {
        var r = req("/api/auth/login", HttpMethod.POST, null, Map.of("identificador", "HU19-CM", "password", ""));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void SE33_comiteNoAutorizaRespuestasDeFamiliaSobreSuCita() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertTrue(esErrorDeCliente(accionCita(cita, comite, "ACEPTAR", Map.of()).getStatusCode().value()));
    }

    @Test
    void SE34_familiaNoCancelaCitaConfirmadaDeOtraFamilia() {
        var propia = caso();
        var ajena = caso();
        long vinAjena = vinculacionAceptada(ajena);
        long citaAjena = datos(solicitarCita(ajena, vinAjena, "DUDAS")).path("id").asLong();
        assertTrue(esErrorDeCliente(accionCita(citaAjena, propia.token(), "CANCELAR", Map.of()).getStatusCode().value()));
    }

    @ParameterizedTest(name = "SE-35-{index} ruta de API sin permiso para la familia: {0}")
    @ValueSource(strings = {"/api/ninos", "/api/scoring/pesos", "/api/scoring/pesos/historial"})
    void SE35_familiaNoAccedeARutasInstitucionales(String ruta) {
        var c = caso();
        assertTrue(esErrorDeCliente(req(ruta, HttpMethod.GET, c.token(), null).getStatusCode().value()), ruta);
    }

    @Test
    void SE36_cierreInstitucionalSoloLoRegistraElComite() {
        var c = caso();
        long id = vinculacionInterrumpida(c);
        var r = req("/api/vinculaciones/" + id + "/cierre", HttpMethod.POST, social,
                Map.of("confirmacionAutoridad", true, "motivo", "Intento de cierre por rol no autorizado"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void SE37_revisionDeInterrupcionSoloLaRegistraElComite() {
        var c = caso();
        long id = vinculacionInterrumpida(c);
        var r = req("/api/vinculaciones/" + id + "/interrupcion/revision", HttpMethod.POST, social,
                Map.of("justificacion", "Intento de revisión por rol no autorizado"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void SE38_familiaNoRegistraInterrupcion() {
        var c = caso();
        long id = vinculacionAceptada(c);
        autorizarInicio(id);
        var r = req("/api/vinculaciones/" + id + "/interrupcion", HttpMethod.POST, c.token(),
                Map.of("motivo", "ADAPTACION", "justificacion", "Intento de interrupción por la familia"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void SE39_psicologoNoEditaAnalisisDeCompatibilidad() {
        var c = caso();
        var r = req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/decisiones", HttpMethod.POST, psicologo,
                Map.of("familia", c.f().getCodigoCaso(), "decision", "ACEPTADA", "motivo", "Intento de decisión no autorizada"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void SE40_familiaNoLeeHistorialDelComite() {
        var c = caso();
        assertTrue(esErrorDeCliente(req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/historial", HttpMethod.GET, c.token(), null)
                .getStatusCode().value()));
    }
}
