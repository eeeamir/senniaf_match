package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fiabilidad del Sprint 9: entradas inválidas, conflictos concurrentes, recuperación tras error
 * y fallos de dependencias. Casos FI-01 a FI-30. Las dependencias que la suite no puede provocar
 * en el entorno de pruebas se marcan como SKIP con su motivo, nunca como aprobadas.
 */
@EnabledIfEnvironmentVariable(named = "HU19_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z0-9_]+")
class FiabilidadSprint9IT extends Sprint9Base {

    // ---------- Entradas inválidas al solicitar citas ----------

    @ParameterizedTest(name = "FI-01-{index} motivo inválido: [{0}]")
    @ValueSource(strings = {"", "INVENTADO", "orientacion-sin-validar"})
    void FI01_motivoInvalidoSeRechazaConMensaje(String motivo) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", motivo, "fecha", h[0], "hora", h[1]));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), r.getBody());
        assertTrue(r.getBody().contains("motivo"));
    }

    @ParameterizedTest(name = "FI-02-{index} vinculacionId no válido: {0}")
    @ValueSource(longs = {0L, -1L})
    void FI02_vinculacionNoValidaSeRechaza(long vinculacionId) {
        var c = caso();
        vinculacionAceptada(c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vinculacionId, "motivo", "DUDAS", "fecha", "2030-01-02", "hora", "10:00"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI03_solicitudSinVinculacionSeRechaza() {
        var c = caso();
        vinculacionAceptada(c);
        var r = req("/api/citas", HttpMethod.POST, c.token(), Map.of("motivo", "DUDAS", "fecha", "2030-01-02", "hora", "10:00"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @ParameterizedTest(name = "FI-04-{index} fecha inválida: [{0}]")
    @ValueSource(strings = {"2026-13-45", "mañana", ""})
    void FI04_fechaInvalidaSeRechaza(String fecha) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", fecha, "hora", "10:00"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @ParameterizedTest(name = "FI-05-{index} hora inválida: [{0}]")
    @ValueSource(strings = {"25:99", "9am"})
    void FI05_horaInvalidaSeRechaza(String hora) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", LocalDate.now(ZONA).plusDays(1).toString(), "hora", hora));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    // ---------- Disponibilidad ----------

    @Test
    void FI06_disponibilidadSinFechaSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        var r = req("/api/citas/disponibilidad?vinculacionId=" + vin + "&duracion=120", HttpMethod.GET, c.token(), null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI07_disponibilidadConIdNoNumericoSeRechaza() {
        var c = caso();
        vinculacionAceptada(c);
        var r = req("/api/citas/disponibilidad?vinculacionId=abc&fecha=2030-01-02&duracion=120", HttpMethod.GET, c.token(), null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @ParameterizedTest(name = "FI-08-{index} duración no permitida para la familia: {0} min")
    @ValueSource(ints = {45, 90, 200})
    void FI08_duracionNoPermitidaSeRechaza(int duracion) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        var r = req("/api/citas/disponibilidad?vinculacionId=" + vin + "&fecha=" + LocalDate.now(ZONA).plusDays(1)
                + "&duracion=" + duracion, HttpMethod.GET, c.token(), null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Duración " + duracion + " no debe aceptarse");
    }

    @Test
    void FI09_disponibilidadDeVinculacionNoHabilitadaSeRechaza() {
        var c = caso();
        long vin = asignar(c);
        var r = req("/api/citas/disponibilidad?vinculacionId=" + vin + "&fecha=" + LocalDate.now(ZONA).plusDays(1)
                + "&duracion=120", HttpMethod.GET, c.token(), null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Sin propuesta aceptada no hay disponibilidad");
    }

    // ---------- Acciones inválidas sobre citas ----------

    @Test
    void FI10_accionDesconocidaSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        var r = accionCita(cita, social, "ELIMINAR", Map.of());
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI11_rechazarSinMotivoSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertTrue(esErrorDeCliente(accionCita(cita, social, "RECHAZAR", Map.of()).getStatusCode().value()));
    }

    @Test
    void FI12_respuestaSinVersionSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        var r = req("/api/citas/" + cita + "/acciones", HttpMethod.POST, social, Map.of("accion", "ACEPTAR"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI13_versionDesactualizadaDaConflicto() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        long viejaVersion = versionCita(cita, social);
        assertEquals(200, accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value());
        var r = req("/api/citas/" + cita + "/acciones", HttpMethod.POST, social,
                Map.of("accion", "RECHAZAR", "motivo", "NO_ATENDER", "version", viejaVersion));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "La versión anterior ya no debe aceptarse");
    }

    @Test
    void FI14_repetirUnaAccionYaAplicadaSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value());
        assertTrue(esErrorDeCliente(accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value()));
    }

    @Test
    void FI15_respuestasSimultaneasSobreLaMismaCitaPermitenUnaSola() throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        List<Integer> codigos = simultaneas(
                () -> accionCita(cita, social, "ACEPTAR", Map.of()),
                () -> accionCita(cita, social, "RECHAZAR", Map.of("motivo", "NO_ATENDER")));
        assertEquals(1, codigos.stream().filter(x -> x == 200).count(), "Solo una respuesta debe aplicarse: " + codigos);
        assertTrue(codigos.stream().allMatch(x -> x < 500), "Ninguna respuesta debe ser un error interno: " + codigos);
    }

    @Test
    void FI16_otroHorarioConDuracionSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        String[] h = primerHueco(vin, c);
        assertEquals(200, accionCita(cita, social, "PROPONER", Map.of("motivo", "AJUSTE_AGENDA", "fecha", h[0], "hora", h[1])).getStatusCode().value());
        var r = accionCita(cita, c.token(), "OTRO_HORARIO", Map.of("fecha", h[0], "hora", h[1], "duracion", 60));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "La duración la fija el profesional");
    }

    @Test
    void FI17_realizadaSobreCitaPendienteSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertTrue(esErrorDeCliente(accionCita(cita, social, "REALIZADA", Map.of()).getStatusCode().value()));
    }

    // ---------- Interrupción, revisión y cierre ----------

    @ParameterizedTest(name = "FI-18-{index} motivo de interrupción inválido: [{0}]")
    @ValueSource(strings = {"", "INVENTADO"})
    void FI18_motivoDeInterrupcionInvalidoSeRechaza(String motivo) {
        var c = caso();
        long id = vinculacionAceptada(c);
        autorizarInicio(id);
        var r = req("/api/vinculaciones/" + id + "/interrupcion", HttpMethod.POST, social,
                Map.of("motivo", motivo, "justificacion", "Descripción suficiente para la prueba"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI19_justificacionDeInterrupcionCortaSeRechaza() {
        var c = caso();
        long id = vinculacionAceptada(c);
        autorizarInicio(id);
        var r = req("/api/vinculaciones/" + id + "/interrupcion", HttpMethod.POST, social,
                Map.of("motivo", "ADAPTACION", "justificacion", "corta"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI20_revisionSinJustificacionSuficienteSeRechaza() {
        var c = caso();
        long id = vinculacionInterrumpida(c);
        var r = req("/api/vinculaciones/" + id + "/interrupcion/revision", HttpMethod.POST, comite,
                Map.of("justificacion", "corta"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI21_cierreSinConfirmacionDeAutoridadSeRechaza() {
        var c = caso();
        long id = vinculacionInterrumpida(c);
        var r = req("/api/vinculaciones/" + id + "/cierre", HttpMethod.POST, comite,
                Map.of("confirmacionAutoridad", false, "motivo", "Motivo suficiente para la prueba"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
        assertNotEquals("CERRADA", datos(req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null)).path("estado").asText());
    }

    @Test
    void FI22_cierreConMotivoCortoSeRechaza() {
        var c = caso();
        long id = vinculacionInterrumpida(c);
        var r = req("/api/vinculaciones/" + id + "/cierre", HttpMethod.POST, comite,
                Map.of("confirmacionAutoridad", true, "motivo", "corto"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    // ---------- Reevaluación del análisis ----------

    @ParameterizedTest(name = "FI-23-{index} categoría de reevaluación inválida: [{0}]")
    @ValueSource(strings = {"INVALIDA", ""})
    void FI23_categoriaDeReevaluacionInvalidaSeRechaza(String categoria) {
        var c = caso();
        var r = req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/decisiones", HttpMethod.POST, comite,
                Map.of("familia", c.f().getCodigoCaso(), "decision", "REEVALUACION",
                        "motivo", "Actualizar datos de la familia", "apartados", List.of("Hogar y red de apoyo"),
                        "categoria", categoria));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    @Test
    void FI24_reevaluacionSinApartadosSeRechaza() {
        var c = caso();
        var r = req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/decisiones", HttpMethod.POST, comite,
                Map.of("familia", c.f().getCodigoCaso(), "decision", "REEVALUACION",
                        "motivo", "Actualizar datos de la familia", "apartados", List.of(), "categoria", "ACLARACION"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    // ---------- Cuerpo malformado ----------

    @Test
    void FI25_jsonMalformadoSeRechaza() {
        var c = caso();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(c.token());
        ResponseEntity<String> r = http.exchange("/api/citas", HttpMethod.POST,
                new HttpEntity<>("{\"motivo\":", h), String.class);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
    }

    // ---------- Recuperación ----------

    @Test
    void FI26_trasUnaSolicitudInvalidaLaSiguienteValidaSigueFuncionando() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        assertTrue(esErrorDeCliente(req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "INVENTADO", "fecha", "2030-01-02", "hora", "10:00")).getStatusCode().value()));
        assertEquals(200, solicitarCita(c, vin, "DUDAS").getStatusCode().value(), "La cita válida debe registrarse después del error");
    }

    @Test
    void FI27_trasUnConflictoSeReintentaConLaVersionVigente() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        long viejaVersion = versionCita(cita, social);
        assertEquals(200, accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value());
        assertTrue(esErrorDeCliente(req("/api/citas/" + cita + "/acciones", HttpMethod.POST, social,
                Map.of("accion", "RECHAZAR", "motivo", "NO_ATENDER", "version", viejaVersion)).getStatusCode().value()));
        assertEquals("CONFIRMADA", estadoCita(cita, social), "El conflicto no debe alterar la cita confirmada");
    }

    // ---------- Fallos de dependencias ----------

    @Test
    void FI28_caidaDeBaseDeDatosSeRecuperaSinPerderDatos() {
        Assumptions.abort("SKIP: requiere provocar la caída de PostgreSQL; no se simula en el entorno de pruebas de la suite");
    }

    @Test
    void FI29_fallaDelServicioDeCorreoNoBloqueaLaSolicitud() {
        Assumptions.abort("SKIP: el correo está deshabilitado en la suite (senniaf.correo.habilitado=false); la falla de red no se provoca");
    }

    @ParameterizedTest(name = "FI-30-{index} cita inexistente: {0}")
    @ValueSource(longs = {999999999L, 0L})
    void FI30_citaInexistenteDaErrorDeCliente(long id) {
        var c = caso();
        var r = req("/api/citas/" + id, HttpMethod.GET, c.token(), null);
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Código " + r.getStatusCode().value());
    }
}
