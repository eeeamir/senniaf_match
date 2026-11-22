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
 * Eficiencia de rendimiento del Sprint 9 (EF-01 a EF-15).
 *
 * Umbrales de tiempo, medidos como p95 (nearest-rank) de las peticiones de cada prueba:
 * - Páginas públicas estáticas (EF-01, EF-02, EF-08): 500 ms.
 * - Lecturas de API ligeras (EF-03, EF-05, EF-09, EF-12, EF-13, EF-14): 1000 ms.
 * - Listados de citas (EF-04, EF-07, EF-15): 1000 ms.
 * - Escrituras (EF-10, EF-11): 2000 ms.
 * - Cálculo de compatibilidad para el Comité (EF-06): 3000 ms.
 *
 * Con 5 peticiones el p95 coincide con el máximo. Cada umbral se aplica a cada nivel de concurrencia.
 * Además de los tiempos, se comprueba que ninguna respuesta sea un error interno (5xx).
 * Cada medición se registra en target/sprint9/rendimiento.csv.
 */
@EnabledIfEnvironmentVariable(named = "HU19_TEST_DB_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/hu19_[a-z0-9_]+")
class EficienciaRendimientoSprint9IT extends Sprint9Base {

    static final Path CSV = Paths.get("target", "sprint9", "rendimiento.csv");

    static final long UMBRAL_PAGINA_MS = 500;
    static final long UMBRAL_LECTURA_MS = 1000;
    static final long UMBRAL_LISTADO_MS = 1000;
    static final long UMBRAL_ESCRITURA_MS = 2000;
    static final long UMBRAL_CALCULO_MS = 3000;

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

    /** p95 por el método nearest-rank: el valor que deja al menos el 95 % de las mediciones por encima o igual. */
    static long p95(List<Medicion> m) {
        List<Long> ordenados = m.stream().map(Medicion::ms).sorted().toList();
        int indice = (int) Math.ceil(0.95 * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, indice));
    }

    /** Falla si el p95 de las mediciones supera el umbral. El mensaje indica la prueba, el p95 y el límite. */
    static void verificarUmbral(String id, List<Medicion> m, long limiteMs) {
        long p95 = p95(m);
        assertTrue(p95 <= limiteMs,
                String.format("%s: p95 = %d ms supera el umbral de %d ms", id, p95, limiteMs));
    }

    /** Mide una petición individual, la registra, exige 200 y aplica el umbral indicado. */
    int medir(String id, String endpoint, String token, String ruta, long limiteMs) {
        long t0 = System.nanoTime();
        int estado = req(ruta, HttpMethod.GET, token, null).getStatusCode().value();
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        registrar(id, endpoint, 1, ms, estado);
        assertEquals(200, estado, endpoint + " debe responder 200");
        verificarUmbral(id, List.of(new Medicion(ms, estado)), limiteMs);
        return estado;
    }

    // ---------- Tiempos individuales por pantalla o endpoint ----------

    @Test
    void EF01_tiempoPaginaPublicaInicio() {
        medir("EF-01", "/inicio.html", null, "/inicio.html", UMBRAL_PAGINA_MS);
    }

    @Test
    void EF02_tiempoPaginaConocerElSistema() {
        medir("EF-02", "/conocer-sistema.html", null, "/conocer-sistema.html", UMBRAL_PAGINA_MS);
    }

    @Test
    void EF03_tiempoOpcionesDeCitasParaFamilia() {
        var c = caso();
        medir("EF-03", "/api/citas/opciones", c.token(), "/api/citas/opciones", UMBRAL_LECTURA_MS);
    }

    @Test
    void EF04_tiempoListadoDeCitasParaTrabajoSocial() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        solicitarCita(c, vin, "DUDAS");
        medir("EF-04", "/api/citas", social, "/api/citas", UMBRAL_LISTADO_MS);
    }

    @Test
    void EF05_tiempoVinculacionesPropiasParaFamilia() {
        var c = caso();
        vinculacionAceptada(c);
        medir("EF-05", "/api/vinculaciones/propias", c.token(), "/api/vinculaciones/propias", UMBRAL_LECTURA_MS);
    }

    @Test
    void EF06_tiempoAnalisisDeCompatibilidadParaComite() {
        caso();
        medir("EF-06", "/api/scoring/casos", comite, "/api/scoring/casos", UMBRAL_CALCULO_MS);
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
        verificarUmbral("EF-07 (" + n + " concurrentes)", m, UMBRAL_LISTADO_MS);
    }

    @ParameterizedTest(name = "EF-08-{index} página pública con {0} peticiones concurrentes")
    @ValueSource(ints = {5, 10, 25})
    void EF08_paginaPublicaBajoConcurrencia(int n) throws Exception {
        List<Medicion> m = concurrente(n, () -> http.getForEntity("/inicio.html", String.class));
        for (var x : m) {
            registrar("EF-08", "/inicio.html", n, x.ms(), x.estado());
            assertEquals(200, x.estado());
        }
        verificarUmbral("EF-08 (" + n + " concurrentes)", m, UMBRAL_PAGINA_MS);
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
        verificarUmbral("EF-09 (" + n + " concurrentes)", m, UMBRAL_LECTURA_MS);
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
        verificarUmbral("EF-10 (" + n + " concurrentes)", m, UMBRAL_ESCRITURA_MS);
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
        verificarUmbral("EF-11 (" + n + " concurrentes)", m, UMBRAL_ESCRITURA_MS);
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
        verificarUmbral("EF-12 (" + n + " concurrentes)", m, UMBRAL_LECTURA_MS);
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
        verificarUmbral("EF-13 (" + n + " concurrentes)", m, UMBRAL_LECTURA_MS);
    }

    @Test
    void EF14_consultasConcurrentesDeOtroTrabajadorSocialNoProvocanErroresInternos() throws Exception {
        int n = 5;
        List<Medicion> m = concurrente(n, () -> req("/api/citas/opciones", HttpMethod.GET, otro, null));
        for (var x : m) registrar("EF-14", "/api/citas/opciones", n, x.ms(), x.estado());
        assertTrue(m.stream().allMatch(x -> x.estado() < 500));
        verificarUmbral("EF-14 (" + n + " concurrentes)", m, UMBRAL_LECTURA_MS);
    }

    @Test
    void EF15_medicionDeListadoConCitasRecientesParaTrabajoSocial() {
        var c = caso();
        long vin = vinculacionAceptada(c);
        solicitarCita(c, vin, "SEGUIMIENTO");
        medir("EF-15", "/api/citas", social, "/api/citas", UMBRAL_LISTADO_MS);
    }
}
