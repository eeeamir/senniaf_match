package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.senniaf.match.model.enums.CondicionSalud;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adecuación funcional del Sprint 9: HU22 (análisis de compatibilidad), PBI41 (estados y
 * presentación), HU23 (portada y página pública) y HU24 (citas con Trabajo Social).
 * Casos AF-01 a AF-40.
 */
@EnabledIfEnvironmentVariable(named = "HU19_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z0-9_]+")
class AdecuacionFuncionalSprint9IT extends Sprint9Base {

    /** Análisis de compatibilidad del caso para el Comité (null si la familia no aparece). */
    JsonNode candidato(Caso c) {
        var r = req("/api/scoring/casos/" + c.n().getCodigoCaso(), HttpMethod.GET, comite, null);
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        for (var f : datos(r).path("familias"))
            if (f.path("codigo").asText().equals(c.f().getCodigoCaso())) return f;
        return null;
    }

    // ---------- HU22: análisis de compatibilidad explicado y actualizable ----------

    @Test
    void AF01_analisisIncluyePuntajeFactoresYMaximoAplicable() {
        var c = caso();
        JsonNode a = candidato(c);
        assertNotNull(a, "El análisis debe listar la familia del caso");
        assertTrue(a.has("puntaje"));
        assertTrue(a.has("estadosFactores"));
        assertTrue(a.has("maximoAplicable"));
        assertTrue(a.path("f").isObject());
    }

    @ParameterizedTest(name = "AF-02-{index} factor de análisis presente: {0}")
    @ValueSource(strings = {"edad", "hermanos", "salud", "apoyo", "logistica"})
    void AF02_factoresDelAnalisisExplicanCadaCriterio(String factor) {
        var c = caso();
        assertTrue(candidato(c).path("f").has(factor), "Falta el factor " + factor);
    }

    @Test
    void AF07_analisisNoGeneraDecisionAutomatica() {
        var c = caso();
        candidato(c);
        assertEquals(0, decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).size(),
                "Consultar el análisis no debe registrar una decisión del Comité");
    }

    @Test
    void AF08_solicitarReevaluacionRegistraHistorialSinBorrarAnalisisPrevio() {
        var c = caso();
        var solicitud = req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/decisiones", HttpMethod.POST, comite,
                Map.of("familia", c.f().getCodigoCaso(), "decision", "REEVALUACION",
                        "motivo", "Solicitar información adicional sobre la red de apoyo"));
        assertEquals(200, solicitud.getStatusCode().value(), solicitud.getBody());
        var historial = datos(req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/historial", HttpMethod.GET, comite, null));
        assertTrue(historial.isArray() && historial.size() >= 1, "El historial debe conservar la reevaluación");
    }

    @Test
    void AF09_actualizarAnalisisRecalculaConLosDatosVigentes() {
        var c = caso();
        c.n().setCondicionesSalud(List.of(CondicionSalud.ASMA));
        ninos.saveAndFlush(c.n());
        int anterior = candidato(c).path("puntaje").asInt();
        var entrevista = Map.<String, Object>of("tipoFamilia", "PAREJA", "redApoyo", List.of("FAMILIA_EXTENDIDA"),
                "provincia", "PANAMA", "disponibilidadViaje", "ALTA", "edadMinimaAceptada", 1,
                "edadMaximaAceptada", 12, "disposicionHermanos", "DISPUESTA",
                "condicionesPuedeAcompanarHoy", List.of("ASMA"));
        var cambio = req("/api/familias/mi-perfil", HttpMethod.PUT, c.token(), entrevista);
        assertEquals(200, cambio.getStatusCode().value(), cambio.getBody());
        assertTrue(candidato(c).path("puntaje").asInt() > anterior, "El análisis debe reflejar la información actualizada");
    }

    @Test
    void AF10_actualizarAnalisisConservaLaDecisionAnterior() {
        var c = caso();
        assertEquals(200, aceptar(c.n(), c.f()).getStatusCode().value());
        var previa = decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).get(0);
        String contexto = previa.getContextoScoring();
        var entrevista = Map.<String, Object>of("tipoFamilia", "PAREJA", "redApoyo", List.of("PAREJA"),
                "provincia", "PANAMA", "disponibilidadViaje", "ALTA", "edadMinimaAceptada", 1,
                "edadMaximaAceptada", 12, "disposicionHermanos", "DISPUESTA");
        assertEquals(200, req("/api/familias/mi-perfil", HttpMethod.PUT, c.token(), entrevista).getStatusCode().value());
        assertEquals(contexto, decisiones.findById(previa.getId()).orElseThrow().getContextoScoring(),
                "La decisión registrada no debe reescribirse al actualizar el análisis");
    }

    // ---------- PBI41: estados, interrupción, cierre y presentación ----------

    @Test
    void AF11_propuestaRechazadaPorFamiliaTieneEstadoPropio() {
        var c = caso();
        long id = asignar(c);
        assertEquals(200, req("/api/vinculaciones/" + id + "/respuesta", HttpMethod.POST, c.token(),
                Map.of("respuesta", "RECHAZAR")).getStatusCode().value());
        String estado = datos(req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null)).path("estado").asText();
        assertNotEquals("CERRADA", estado, "Rechazar una propuesta no equivale a un cierre institucional");
        assertNotEquals("RESPUESTA_ACEPTADA", estado);
        assertTrue(Set.of("RECHAZADA_POR_FAMILIA", "RESPUESTA_RECHAZADA_PENDIENTE_CIERRE").contains(estado), estado);
    }

    @Test
    void AF12_interrupcionNoEquivaleAUnCierreInstitucional() {
        var c = caso();
        long id = vinculacionInterrumpida(c);
        String estado = datos(req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null)).path("estado").asText();
        assertNotEquals("CERRADA", estado);
    }

    @Test
    void AF13_cierreInstitucionalDejaEstadoCerrada() {
        var c = caso();
        long id = vinculacionCerrada(c);
        assertEquals("CERRADA", datos(req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null)).path("estado").asText());
    }

    @Test
    void AF14_cierreConservaRespuestaFamiliarAceptada() {
        var c = caso();
        long id = vinculacionCerrada(c);
        assertEquals("ACEPTADA", datos(req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null)).path("respuestaFamiliar").asText());
    }

    @Test
    void AF15_citasExplicanQueLaVinculacionCerradaNoPermiteSolicitarCitas() {
        var c = caso();
        vinculacionCerrada(c);
        var v = datos(req("/api/citas/opciones", HttpMethod.GET, c.token(), null)).path("vinculaciones").get(0);
        assertFalse(v.path("habilitada").asBoolean());
        assertTrue(v.path("motivoNoDisponible").asText().contains("cerrada"));
    }

    @Test
    void AF16_citasExplicanQueLaPropuestaDebeEstarAceptadaAntesDeSolicitar() {
        var c = caso();
        asignar(c);
        var v = datos(req("/api/citas/opciones", HttpMethod.GET, c.token(), null)).path("vinculaciones").get(0);
        assertFalse(v.path("habilitada").asBoolean());
        assertTrue(v.path("motivoNoDisponible").asText().contains("aceptada"));
    }

    // ---------- HU23: portada pública y página "Conocer el sistema" ----------

    @Test
    void AF17_portadaTieneEnlaceConocerElSistema() {
        var r = http.getForEntity("/inicio.html", String.class);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody().contains("Conocer el sistema"));
    }

    @ParameterizedTest(name = "AF-18-{index} la página explica el rol: {0}")
    @ValueSource(strings = {"Comité", "Trabajo Social", "Psicología", "Familias"})
    void AF18_paginaConocerElSistemaExplicaCadaRol(String rol) {
        var r = http.getForEntity("/conocer-sistema.html", String.class);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody().contains(rol), "Falta la explicación del rol " + rol);
    }

    @ParameterizedTest(name = "AF-19-{index} página pública accesible sin sesión: {0}")
    @ValueSource(strings = {"/", "/inicio.html", "/conocer-sistema.html"})
    void AF19_paginasPublicasSeAbrenSinSesion(String ruta) {
        assertEquals(200, http.getForEntity(ruta, String.class).getStatusCode().value());
    }

    @Test
    void AF20_conocerElSistemaEsUnDocumentoIndependiente() {
        var body = http.getForEntity("/conocer-sistema.html", String.class).getBody();
        assertNotNull(body);
        assertTrue(body.contains("<html") && body.contains("<title"), "La página debe ser un documento propio");
    }

    // ---------- HU24: solicitud, respuesta y contrapropuesta de citas ----------

    @Test
    void AF21_familiaSolicitaCitaQuedaPendiente() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        var r = solicitarCita(c, vin, "ORIENTACION");
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        assertEquals("PENDIENTE", datos(r).path("estado").asText());
    }

    @Test
    void AF22_trabajadorSocialProponeOtroHorarioYCambiaEstado() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "SEGUIMIENTO")).path("id").asLong();
        String[] h = primerHueco(vin, c);
        var r = accionCita(cita, social, "PROPONER", Map.of("motivo", "AJUSTE_AGENDA", "fecha", h[0], "hora", h[1]));
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        assertEquals("CAMBIO_PROPUESTO", estadoCita(cita, social));
    }

    @Test
    void AF23_familiaAceptaLaPropuestaYQuedaConfirmada() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ENCUENTRO")).path("id").asLong();
        String[] h = primerHueco(vin, c);
        assertEquals(200, accionCita(cita, social, "PROPONER", Map.of("motivo", "NUEVO_HORARIO", "fecha", h[0], "hora", h[1])).getStatusCode().value());
        assertEquals(200, accionCita(cita, c.token(), "ACEPTAR_PROPUESTA", Map.of()).getStatusCode().value());
        assertEquals("CONFIRMADA", estadoCita(cita, c.token()));
    }

    @Test
    void AF24_familiaPideOtroHorarioYLaSolicitudVuelveAPendiente() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        String[] h = primerHueco(vin, c);
        assertEquals(200, accionCita(cita, social, "PROPONER", Map.of("motivo", "SIN_DISPONIBILIDAD", "fecha", h[0], "hora", h[1])).getStatusCode().value());
        assertEquals(200, accionCita(cita, c.token(), "OTRO_HORARIO", Map.of("fecha", h[0], "hora", h[1])).getStatusCode().value());
        assertEquals("PENDIENTE", estadoCita(cita, c.token()));
    }

    @Test
    void AF25_trabajadorSocialAceptaSolicitudPendienteYQuedaConfirmada() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "OBSERVACION")).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value());
        assertEquals("CONFIRMADA", estadoCita(cita, social));
    }

    @Test
    void AF26_trabajadorSocialRechazaConMensajeExplicativo() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ORIENTACION")).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "RECHAZAR", Map.of("motivo", "COORDINACION_PREVIA")).getStatusCode().value());
        var detalle = datos(req("/api/citas/" + cita, HttpMethod.GET, c.token(), null));
        assertEquals("RECHAZADA", detalle.path("estado").asText());
        assertFalse(detalle.path("respuesta").asText().isBlank(), "El rechazo debe explicar el motivo a la familia");
    }

    @Test
    void AF27_familiaCancelaSolicitudPendiente() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ENCUENTRO")).path("id").asLong();
        assertEquals(200, accionCita(cita, c.token(), "CANCELAR", Map.of()).getStatusCode().value());
        assertEquals("CANCELADA", estadoCita(cita, c.token()));
    }

    @Test
    void AF28_trabajadorSocialNoPuedeRegistrarRealizadaAntesDeLaCita() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "SEGUIMIENTO")).path("id").asLong();
        ResponseEntity<String> r = accionCita(cita, social, "REALIZADA", Map.of());
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Una cita pendiente no puede marcarse como realizada");
    }

    @ParameterizedTest(name = "AF-29-{index} motivo de cita válido: {0}")
    @ValueSource(strings = {"ORIENTACION", "SEGUIMIENTO", "ENCUENTRO", "OBSERVACION", "DUDAS"})
    void AF29_cadaMotivoDeCitaSeRegistra(String motivo) {
        var c = caso();
        long vin = vinculacionAceptada(c);
        var r = solicitarCita(c, vin, motivo);
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        assertEquals(motivo, datos(r).path("motivo").asText());
    }

    @Test
    void AF30_familiaVeSusCitasEnElListado() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        var listado = datos(req("/api/citas", HttpMethod.GET, c.token(), null));
        boolean esta = false;
        for (var it : listado) if (it.path("id").asLong() == cita) esta = true;
        assertTrue(esta, "La familia debe ver su solicitud en el listado");
    }

    @Test
    void AF31_trabajadorSocialVeLaCitaDeSuVinculacion() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ORIENTACION")).path("id").asLong();
        assertEquals(200, req("/api/citas/" + cita, HttpMethod.GET, social, null).getStatusCode().value());
    }

    @Test
    void AF32_disponibilidadDevuelveHorasEnFormatoHHmm() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        assertTrue(h[1].matches("\\d{2}:\\d{2}"), "La hora debe venir en formato HH:mm, llegó " + h[1]);
    }

    @Test
    void AF33_historialDeCitaRegistraLaSolicitud() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ENCUENTRO")).path("id").asLong();
        var historial = datos(req("/api/citas/" + cita, HttpMethod.GET, c.token(), null)).path("historial");
        assertTrue(historial.isArray() && historial.size() >= 1, "La solicitud debe quedar en el historial");
    }

    @Test
    void AF34_historialRegistraLaPropuestaDespuesDeLaSolicitud() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "SEGUIMIENTO")).path("id").asLong();
        String[] h = primerHueco(vin, c);
        assertEquals(200, accionCita(cita, social, "PROPONER", Map.of("motivo", "AJUSTE_AGENDA", "fecha", h[0], "hora", h[1])).getStatusCode().value());
        var historial = datos(req("/api/citas/" + cita, HttpMethod.GET, social, null)).path("historial");
        assertTrue(historial.size() >= 2, "Solicitud y propuesta deben quedar registradas");
    }

    @Test
    void AF35_familiaNoPuedeSolicitarCitaCuandoLaPropuestaNoEstaAceptada() {
        var c = caso();
        long vin = asignar(c);
        String manana = java.time.LocalDate.now(ZONA).plusDays(1).toString();
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", manana, "hora", "10:00"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Sin propuesta aceptada no se pueden solicitar citas");
    }

    @Test
    void AF36_solicitudDuplicadaAbiertaSeRechaza() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        assertEquals(200, solicitarCita(c, vin, "DUDAS").getStatusCode().value());
        var segunda = solicitarCita(c, vin, "DUDAS");
        assertTrue(esErrorDeCliente(segunda.getStatusCode().value()), "No debe haber dos solicitudes abiertas");
    }

    @Test
    void AF37_trabajadorSocialDeOtroCasoNoVeLaCita() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ORIENTACION")).path("id").asLong();
        assertTrue(esErrorDeCliente(req("/api/citas/" + cita, HttpMethod.GET, otro, null).getStatusCode().value()));
    }

    @Test
    void AF38_citaConfirmadaNoPuedeRechazarse() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value());
        assertTrue(esErrorDeCliente(accionCita(cita, social, "RECHAZAR", Map.of("motivo", "NO_ATENDER")).getStatusCode().value()),
                "Una cita ya confirmada no puede rechazarse");
    }

    @Test
    void AF39_motivoDeRechazoInvalidoSeExplicaComoError() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        var r = accionCita(cita, social, "RECHAZAR", Map.of("motivo", "MOTIVO_INVENTADO"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()));
        assertTrue(r.getBody().contains("motivo"), "El error debe indicar el motivo inválido");
    }

    @Test
    void AF40_listadoDelTrabajadorSocialIncluyeLasSolicitudesPendientes() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ORIENTACION")).path("id").asLong();
        var listado = datos(req("/api/citas", HttpMethod.GET, social, null));
        boolean esta = false;
        for (var it : listado) if (it.path("id").asLong() == cita) esta = true;
        assertTrue(esta);
    }
}
