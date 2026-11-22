package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.senniaf.match.model.CombinacionExcluida;
import com.senniaf.match.repository.CombinacionExcluidaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regresión de defectos del Sprint 9. Cada prueba verifica un comportamiento corregido o
 * requerido por PBI41, HU22 o HU24, y debe seguir pasando. El tablero no entregó IDs de bugs
 * en la brief: estas pruebas se nombran por comportamiento. Si aparece un defecto nuevo, la
 * prueba que lo detecta queda en FAIL con su mensaje, y no se modifica para que pase.
 * Casos DE-01 a DE-14.
 */
@EnabledIfEnvironmentVariable(named = "HU19_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z0-9_]+")
class DefectosSprint9IT extends Sprint9Base {

    @Autowired CombinacionExcluidaRepository exclusiones;

    JsonNode candidato(Caso c) {
        var r = req("/api/scoring/casos/" + c.n().getCodigoCaso(), HttpMethod.GET, comite, null);
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        for (var f : datos(r).path("familias"))
            if (f.path("codigo").asText().equals(c.f().getCodigoCaso())) return f;
        return null;
    }

    JsonNode liberar(long id, String tipo) {
        var r = req("/api/vinculaciones/" + id + "/disponibilidad", HttpMethod.POST, comite,
                Map.of("expediente", tipo, "justificacion", "Se autoriza de manera independiente la disponibilidad y elegibilidad"));
        assertEquals(200, r.getStatusCode().value(), r.getBody());
        return datos(r);
    }

    // ---------- PBI41: interrupción, revisión y cierre ----------

    @Test
    void DE01_interrupcionHistoricaMantieneElParBloqueadoHastaRevisarAmbosExpedientes() {
        var c = caso();
        long id = vinculacionCerrada(c);
        assertNull(candidato(c), "Tras la interrupción la pareja no debe aparecer como elegible");
        liberar(id, "NINO");
        assertNull(candidato(c), "Revisar solo el expediente del niño no basta");
        liberar(id, "FAMILIA");
        assertNotNull(candidato(c), "Con ambas revisiones la pareja vuelve a ser evaluable");
    }

    @Test
    void DE02_cierreInstitucionalConservaRespuestaFamiliarYRegistroDeInterrupcion() {
        var c = caso();
        long id = vinculacionCerrada(c);
        var institucional = datos(req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null));
        assertEquals("CERRADA", institucional.path("estado").asText());
        assertEquals("ACEPTADA", institucional.path("respuestaFamiliar").asText());
        assertEquals("TRABAJADOR_SOCIAL", institucional.path("seguimientoFamiliar").path("interrupcionRol").asText());
        assertFalse(institucional.path("seguimientoFamiliar").path("interrupcionRegistradaEl").isNull());
    }

    @Test
    void DE03_citasNoSePuedenSolicitarSobreUnCierreInstitucional() {
        var c = caso();
        long id = vinculacionCerrada(c);
        var r = req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", id, "motivo", "DUDAS", "fecha", "2030-01-02", "hora", "10:00"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()), "Una vinculación cerrada no admite nuevas citas");
    }

    @Test
    void DE04_cierreInstitucionalNoSeConfundeConPropuestaNoAceptadaEnLasCitas() {
        var c = caso();
        asignar(c);
        var v = datos(req("/api/citas/opciones", HttpMethod.GET, c.token(), null)).path("vinculaciones").get(0);
        assertFalse(v.path("habilitada").asBoolean());
        assertTrue(v.path("motivoNoDisponible").asText().contains("aceptada"),
                "La propuesta sin aceptar debe explicarse como tal, no como cierre");
        assertFalse(v.path("motivoNoDisponible").asText().contains("cerrada"));
    }

    @Test
    void DE05_restriccionExpresaPersisteTrasRevisarExpedientes() {
        var c = caso();
        long id = vinculacionCerrada(c);
        liberar(id, "NINO");
        liberar(id, "FAMILIA");
        var e = new CombinacionExcluida();
        e.codigoNino = c.n().getCodigoCaso();
        e.codigoFamilia = c.f().getCodigoCaso();
        e.origen = "COMITE";
        e.responsable = "HU19-CM";
        e.fecha = LocalDateTime.now();
        exclusiones.saveAndFlush(e);
        assertNull(candidato(c));
        assertTrue(esErrorDeCliente(aceptar(c.n(), c.f()).getStatusCode().value()));
        assertTrue(exclusiones.existsById(e.id), "La restricción expresa no debe desaparecer al revisar expedientes");
    }

    @Test
    void DE06_cierreYCitasDistinguenUnaVinculacionCerradaDeUnaPendiente() {
        var c = caso();
        long id = vinculacionAceptada(c);
        var activa = datos(req("/api/citas/opciones", HttpMethod.GET, c.token(), null)).path("vinculaciones").get(0);
        assertTrue(activa.path("habilitada").asBoolean());
        assertEquals("", activa.path("motivoNoDisponible").asText());
        autorizarInicio(id);
        assertEquals(200, req("/api/vinculaciones/" + id + "/cierre", HttpMethod.POST, comite,
                Map.of("confirmacionAutoridad", true, "motivo", "Cierre sintético para la regresión DE06")).getStatusCode().value());
        var cerrada = datos(req("/api/citas/opciones", HttpMethod.GET, c.token(), null)).path("vinculaciones").get(0);
        assertFalse(cerrada.path("habilitada").asBoolean());
        assertTrue(cerrada.path("motivoNoDisponible").asText().contains("cerrada"));
    }

    // ---------- HU22: historial del análisis ----------

    @Test
    void DE07_reevaluacionNoDuplicaLaDecisionAceptada() {
        var c = caso();
        assertEquals(200, aceptar(c.n(), c.f()).getStatusCode().value());
        var r = req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/decisiones", HttpMethod.POST, comite,
                Map.of("familia", c.f().getCodigoCaso(), "decision", "REEVALUACION",
                        "motivo", "Solicitar aclaración de la red de apoyo", "apartados", List.of("Hogar y red de apoyo"),
                        "categoria", "ACLARACION"));
        assertTrue(esErrorDeCliente(r.getStatusCode().value()),
                "Con aceptación registrada no se admite reevaluación: " + r.getBody());
        assertEquals(1, decisiones.findByCodigoFamiliaOrderByFechaAsc(c.f().getCodigoCaso()).stream()
                .filter(d -> "ACEPTADA".equals(d.getDecision())).count(),
                "La reevaluación no debe crear una segunda aceptación");
    }

    @Test
    void DE08_historialDelAnalisisConservaLasDecisionesAnteriores() {
        var c = caso();
        assertEquals(200, aceptar(c.n(), c.f()).getStatusCode().value());
        var historial = datos(req("/api/scoring/casos/" + c.n().getCodigoCaso() + "/historial", HttpMethod.GET, comite, null));
        assertTrue(historial.isArray() && historial.size() >= 1, "El historial debe conservar la decisión anterior");
    }

    // ---------- HU24: estados terminales y disponibilidad ----------

    @Test
    void DE09_citaRechazadaEsTerminal() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "RECHAZAR", Map.of("motivo", "NO_ATENDER")).getStatusCode().value());
        assertTrue(esErrorDeCliente(accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value()),
                "Una cita rechazada no puede volver a confirmarse");
        assertEquals("RECHAZADA", estadoCita(cita, social));
    }

    @Test
    void DE10_citaCanceladaEsTerminal() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        assertEquals(200, accionCita(cita, c.token(), "CANCELAR", Map.of()).getStatusCode().value());
        assertTrue(esErrorDeCliente(accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value()),
                "Una cita cancelada no puede volver a confirmarse");
        assertEquals("CANCELADA", estadoCita(cita, c.token()));
    }

    @Test
    void DE11_horarioConfirmadoDesapareceDeLaDisponibilidad() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        long cita = datos(req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", h[0], "hora", h[1]))).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "ACEPTAR", Map.of()).getStatusCode().value());
        var horas = datos(req("/api/citas/disponibilidad?vinculacionId=" + vin + "&fecha=" + h[0] + "&duracion=120",
                HttpMethod.GET, c.token(), null));
        for (var x : horas) assertNotEquals(h[1], x.asText(), "Un horario confirmado no debe ofrecerse de nuevo");
    }

    @Test
    void DE12_historialDeCitaRegistraLaRespuestaDelRechazo() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ORIENTACION")).path("id").asLong();
        assertEquals(200, accionCita(cita, social, "RECHAZAR", Map.of("motivo", "COORDINACION_PREVIA")).getStatusCode().value());
        var historial = datos(req("/api/citas/" + cita, HttpMethod.GET, c.token(), null)).path("historial");
        assertTrue(historial.size() >= 2, "Solicitud y rechazo deben quedar en el historial");
    }

    @Test
    void DE13_listadoDeCitasNoMezclaLasDeOtraVinculacion() {
        var c = caso();
        var ajena = caso();
        long vinC = vinculacionAceptada(c);
        long vinA = vinculacionAceptada(ajena);
        long citaC = datos(solicitarCita(c, vinC, "DUDAS")).path("id").asLong();
        long citaA = datos(solicitarCita(ajena, vinA, "DUDAS")).path("id").asLong();
        var listado = datos(req("/api/citas", HttpMethod.GET, c.token(), null));
        boolean incluyeAjena = false;
        for (var it : listado) if (it.path("id").asLong() == citaA) incluyeAjena = true;
        assertFalse(incluyeAjena, "La familia no debe ver la cita de otra familia");
        assertTrue(citaC > 0);
    }

    @Test
    void DE14_cierreNoBorraLosAntecedentesDelCaso() {
        var c = caso();
        long id = vinculacionCerrada(c);
        var detalle = req("/api/vinculaciones/" + id, HttpMethod.GET, comite, null);
        assertEquals(200, detalle.getStatusCode().value());
        assertTrue(detalle.getBody() != null && detalle.getBody().contains("CERRADA"),
                "El detalle debe seguir disponible y mostrar el cierre");
    }
}
