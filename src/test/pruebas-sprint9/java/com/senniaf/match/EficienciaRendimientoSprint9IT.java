package com.senniaf.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Eficiencia de rendimiento del Sprint 9. Mide tiempos de respuesta y registra cada medición en
 * target/sprint9/rendimiento.csv para el informe. El informe no define umbrales, por lo que estas
 * pruebas NO fijan límites de tiempo: solo comprueban que las respuestas sean correctas y que
 * ninguna petición concurrente produzca un error interno (5xx). Casos EF-01 a EF-15.
 */
@EnabledIfEnvironmentVariable(named = "HU19_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z0-9_]+")
class EficienciaRendimientoSprint9IT extends Sprint9Base {

    static final Path CSV = Paths.get("target", "sprint9", "rendimiento.csv");

    record Medicion(long ms, int estado) {}

    static synchronized void registrar(String id, String endpoint, int concurrencia, long ms, int estado) {
        try {
            Files.createDirectories(CSV.getParent());
            boolean nuevo = !Files.exists(CSV);
            try (var w = Files.newBufferedWriter(CSV, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (nuevo) w.write("id,endpoint,concurrencia,duracion_ms,estado_http\n");
                w.write(id + "," + endpoint + "," + concurrencia + "," + ms + "," + estado + "\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Ejecuta n peticiones que arrancan a la vez y devuelve el tiempo y el estado de cada una. */
    List<Medicion> concurrente(int n, Supplier<ResponseEntity<String>> peticion) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch listos = new CountDownLatch(n);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Medicion>> futuros = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    long t0 = System.nanoTime();
                    int estado = peticion.get().getStatusCode().value();
                    return new Medicion((System.nanoTime() - t0) / 1_000_000L, estado);
                }));
            }
            assertTrue(listos.await(15, TimeUnit.SECONDS), "No arrancaron todas las peticiones concurrentes");
            salida.countDown();
            List<Medicion> resultados = new ArrayList<>();
            for (var f : futuros) resultados.add(f.get(60, TimeUnit.SECONDS));
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Mide una petición individual y la registra. */
    int medir(String id, String endpoint, String token, String ruta) {
        long t0 = System.nanoTime();
        int estado = req(ruta, HttpMethod.GET, token, null).getStatusCode().value();
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        registrar(id, endpoint, 1, ms, estado);
        assertEquals(200, estado, endpoint + " debe responder 200");
        return estado;
    }

    // ---------- Tiempos individuales por pantalla o endpoint ----------

    @Test
    void EF01_tiempoPaginaPublicaInicio() {
        medir("EF-01", "/inicio.html", null, "/inicio.html");
    }

    @Test
    void EF02_tiempoPaginaConocerElSistema() {
        medir("EF-02", "/conocer-sistema.html", null, "/conocer-sistema.html");
    }

    @Test
    void EF03_tiempoOpcionesDeCitasParaFamilia() {
        var c = caso();
        medir("EF-03", "/api/citas/opciones", c.token(), "/api/citas/opciones");
    }

    @Test
    void EF04_tiempoListadoDeCitasParaTrabajoSocial() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        solicitarCita(c, vin, "DUDAS");
        medir("EF-04", "/api/citas", social, "/api/citas");
    }

    @Test
    void EF05_tiempoVinculacionesPropiasParaFamilia() {
        var c = caso();
        vinculacionAceptada(c);
        medir("EF-05", "/api/vinculaciones/propias", c.token(), "/api/vinculaciones/propias");
    }

    @Test
    void EF06_tiempoAnalisisDeCompatibilidadParaComite() {
        var c = caso();
        medir("EF-06", "/api/scoring/casos", comite, "/api/scoring/casos");
    }

    // ---------- Concurrencia de lectura (N peticiones simultáneas) ----------

    @ParameterizedTest(name = "EF-07-{index} GET /api/citas con {0} peticiones concurrentes")
    @ValueSource(ints = {5, 10, 25})
    void EF07_listadoDeCitasBajoConcurrencia(int n) throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        solicitarCita(c, vin, "DUDAS");
        List<Medicion> m = concurrente(n, () -> req("/api/citas", HttpMethod.GET, social, null));
        for (var x : m) {
            registrar("EF-07", "/api/citas", n, x.ms(), x.estado());
            assertTrue(x.estado() < 500, "Error interno bajo concurrencia: " + x.estado());
            assertEquals(200, x.estado());
        }
    }

    @ParameterizedTest(name = "EF-08-{index} página pública con {0} peticiones concurrentes")
    @ValueSource(ints = {5, 10, 25})
    void EF08_paginaPublicaBajoConcurrencia(int n) throws Exception {
        List<Medicion> m = concurrente(n, () -> http.getForEntity("/inicio.html", String.class));
        for (var x : m) {
            registrar("EF-08", "/inicio.html", n, x.ms(), x.estado());
            assertEquals(200, x.estado());
        }
    }

    @ParameterizedTest(name = "EF-09-{index} opciones de citas con {0} peticiones concurrentes")
    @ValueSource(ints = {5, 10, 25})
    void EF09_opcionesDeCitasBajoConcurrencia(int n) throws Exception {
        var c = caso();
        vinculacionAceptada(c);
        List<Medicion> m = concurrente(n, () -> req("/api/citas/opciones", HttpMethod.GET, c.token(), null));
        for (var x : m) {
            registrar("EF-09", "/api/citas/opciones", n, x.ms(), x.estado());
            assertEquals(200, x.estado());
        }
    }

    // ---------- Concurrencia de escritura ----------

    @Test
    void EF10_solicitudesSimultaneasDeLaMismaFamiliaDejanUnaSolaAbierta() throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        int n = 10;
        List<Medicion> m = concurrente(n, () -> req("/api/citas", HttpMethod.POST, c.token(),
                Map.of("vinculacionId", vin, "motivo", "DUDAS", "fecha", h[0], "hora", h[1])));
        for (var x : m) registrar("EF-10", "/api/citas POST", n, x.ms(), x.estado());
        long exitos = m.stream().filter(x -> x.estado() == 200).count();
        assertEquals(1, exitos, "Solo una solicitud abierta por vinculación: " + m);
        assertTrue(m.stream().allMatch(x -> x.estado() < 500), "Ninguna respuesta debe ser un error interno");
    }

    @Test
    void EF11_respuestasSimultaneasDeTrabajoSocialNoProvocanErroresInternos() throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "DUDAS")).path("id").asLong();
        int n = 10;
        List<Medicion> m = concurrente(n, () -> accionCita(cita, social, "ACEPTAR", Map.of()));
        for (var x : m) registrar("EF-11", "/api/citas/acciones", n, x.ms(), x.estado());
        assertEquals(1, m.stream().filter(x -> x.estado() == 200).count(), "Solo una respuesta debe aplicarse: " + m);
        assertTrue(m.stream().allMatch(x -> x.estado() < 500));
    }

    @Test
    void EF12_lecturasDeDetalleConcurrentesNoAlteranLaCita() throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        long cita = datos(solicitarCita(c, vin, "ORIENTACION")).path("id").asLong();
        int n = 10;
        List<Medicion> m = concurrente(n, () -> req("/api/citas/" + cita, HttpMethod.GET, social, null));
        for (var x : m) registrar("EF-12", "/api/citas/{id}", n, x.ms(), x.estado());
        assertTrue(m.stream().allMatch(x -> x.estado() == 200));
        assertEquals("PENDIENTE", estadoCita(cita, social));
    }

    @Test
    void EF13_consultasDeDisponibilidadConcurrentesDevuelvenLaMismaRespuesta() throws Exception {
        var c = caso();
        long vin = vinculacionAceptada(c);
        String[] h = primerHueco(vin, c);
        String ruta = "/api/citas/disponibilidad?vinculacionId=" + vin + "&fecha=" + h[0] + "&duracion=120";
        int n = 10;
        List<Medicion> m = concurrente(n, () -> req(ruta, HttpMethod.GET, c.token(), null));
        for (var x : m) registrar("EF-13", "/api/citas/disponibilidad", n, x.ms(), x.estado());
        assertTrue(m.stream().allMatch(x -> x.estado() == 200));
    }

    @Test
    void EF14_consultasConcurrentesDeOtroTrabajadorSocialNoProvocanErroresInternos() throws Exception {
        int n = 5;
        List<Medicion> m = concurrente(n, () -> req("/api/citas/opciones", HttpMethod.GET, otro, null));
        for (var x : m) registrar("EF-14", "/api/citas/opciones", n, x.ms(), x.estado());
        assertTrue(m.stream().allMatch(x -> x.estado() < 500));
    }

    @Test
    void EF15_medicionDeListadoConCitasRecientesParaTrabajoSocial() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        solicitarCita(c, vin, "SEGUIMIENTO");
        medir("EF-15", "/api/citas", social, "/api/citas");
    }
}
