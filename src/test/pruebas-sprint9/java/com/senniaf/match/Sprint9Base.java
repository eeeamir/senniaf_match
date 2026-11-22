package com.senniaf.match;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base común de la suite de pruebas del Sprint 9 (HU22, PBI41, HU23, HU24).
 * Reutiliza las fixtures sintéticas de SoportePostgresIT. Cada caso crea sus propios
 * expedientes con códigos únicos, por lo que no depende del orden ni de otros casos.
 */
abstract class Sprint9Base extends SoportePostgresIT {

    static final ZoneId ZONA = ZoneId.of("America/Panama");

    /** Horario solo para pruebas: lunes a viernes de 08:00 a 16:00, cada 30 minutos (igual que el perfil citas-demo). */
    @DynamicPropertySource
    static void configurarCitas(DynamicPropertyRegistry r) {
        r.add("senniaf.citas.franjas", () -> "1@08:00-16:00,2@08:00-16:00,3@08:00-16:00,4@08:00-16:00,5@08:00-16:00");
        r.add("senniaf.citas.intervalo-minutos", () -> 30);
    }

    /** Vinculación aceptada por el Comité y confirmada por la familia. */
    long vinculacionAceptada(Caso c) {
        long id = asignar(c);
        assertEquals(200, req("/api/vinculaciones/" + id + "/respuesta", HttpMethod.POST, c.token(),
                Map.of("respuesta", "ACEPTAR")).getStatusCode().value());
        return id;
    }

    /** Vinculación con interrupción registrada por Trabajo Social (antes del cierre). */
    long vinculacionInterrumpida(Caso c) {
        long id = vinculacionAceptada(c);
        autorizarInicio(id);
        assertEquals(200, req("/api/vinculaciones/" + id + "/interrupcion", HttpMethod.POST, social,
                Map.of("motivo", "ADAPTACION",
                        "justificacion", "Interrupción sintética de la suite Sprint 9")).getStatusCode().value());
        return id;
    }

    /** Vinculación con cierre institucional registrado por el Comité. */
    long vinculacionCerrada(Caso c) {
        long id = vinculacionInterrumpida(c);
        assertEquals(200, req("/api/vinculaciones/" + id + "/cierre", HttpMethod.POST, comite,
                Map.of("confirmacionAutoridad", true,
                        "motivo", "Cierre institucional sintético con antecedentes conservados")).getStatusCode().value());
        return id;
    }

    /** Primer hueco disponible del profesional asignado en los próximos 21 días: {fecha, hora}. */
    String[] primerHueco(long vinculacionId, Caso c) {
        LocalDate hoy = LocalDate.now(ZONA);
        for (int i = 1; i <= 21; i++) {
            String fecha = hoy.plusDays(i).toString();
            ResponseEntity<String> r = req("/api/citas/disponibilidad?vinculacionId=" + vinculacionId
                    + "&fecha=" + fecha + "&duracion=120", HttpMethod.GET, c.token(), null);
            if (r.getStatusCode().is2xxSuccessful()) {
                JsonNode horas = datos(r);
                if (horas.isArray() && horas.size() > 0) return new String[]{fecha, horas.get(0).asText()};
            }
        }
        throw new AssertionError("No hay horarios disponibles en los próximos 21 días para la vinculación " + vinculacionId);
    }

    /** La familia solicita una cita con el motivo indicado usando el primer hueco disponible. */
    ResponseEntity<String> solicitarCita(Caso c, long vinculacionId, String motivo) {
        String[] h = primerHueco(vinculacionId, c);
        return req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vinculacionId, "motivo", motivo, "fecha", h[0], "hora", h[1]));
    }

    /** Versión actual de la cita, leída desde el servidor antes de cada acción. */
    long versionCita(long citaId, String token) {
        return datos(req("/api/citas/" + citaId, HttpMethod.GET, token, null)).path("version").asLong();
    }

    /** Acción sobre una cita (ACEPTAR, RECHAZAR, PROPONER, ACEPTAR_PROPUESTA, OTRO_HORARIO, CANCELAR, REALIZADA). */
    ResponseEntity<String> accionCita(long citaId, String token, String accion, Map<String, Object> extra) {
        long version = versionCita(citaId, token);
        Map<String, Object> cuerpo = new LinkedHashMap<>(extra);
        cuerpo.put("accion", accion);
        cuerpo.put("version", version);
        return req("/api/citas/" + citaId + "/acciones", HttpMethod.POST, token, cuerpo);
    }

    String estadoCita(long citaId, String token) {
        return datos(req("/api/citas/" + citaId, HttpMethod.GET, token, null)).path("estado").asText();
    }

    /** True para respuestas de error del cliente (4xx). Un 5xx nunca es aceptable en estas pruebas. */
    static boolean esErrorDeCliente(int codigo) {
        return codigo >= 400 && codigo < 500;
    }
}
