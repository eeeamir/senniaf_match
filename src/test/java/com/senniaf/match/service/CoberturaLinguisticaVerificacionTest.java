package com.senniaf.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.service.AnalizadorTextoCompatibilidad.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Auditoría semántica: sus fallos son hallazgos, no autorizan a cambiar el motor. */
class CoberturaLinguisticaVerificacionTest {
    private static final List<Map<String, Object>> RESULTADOS = Collections.synchronizedList(new ArrayList<>());
    private final AnalizadorTextoCompatibilidad analizador = new AnalizadorTextoCompatibilidad();
    private final ScoringService motor = new ScoringService(mock(PerfilNinoRepository.class),
            mock(PerfilFamiliaRepository.class), mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));

    record Caso(String id, String concepto, Dominio dominio, String necesidad, String texto,
                double esperado, String grupo, String fuente) {
        @Override public String toString() { return id + ":" + fuente; }
    }

    static Stream<Caso> capacidades() throws Exception {
        try (var lineas = Files.lines(Path.of("src/test/resources/linguistica/capacidades.tsv"))) {
            return lineas.skip(1).filter(s -> !s.isBlank()).map(s -> s.split("\t", -1))
                    .map(v -> new Caso(v[0], v[1], Dominio.valueOf(v[2]), v[3], v[4],
                            Double.parseDouble(v[5]), v[6], "familia")).toList().stream();
        }
    }

    static Stream<Caso> necesidades() throws Exception {
        try (var lineas = Files.lines(Path.of("src/test/resources/linguistica/necesidades.tsv"))) {
            return lineas.skip(1).filter(s -> !s.isBlank()).map(s -> s.split("\t", -1))
                    .flatMap(v -> Stream.of("perfil", "recomendacion").map(fuente ->
                            new Caso(v[0], v[1], Dominio.SALUD, "", v[2], Double.parseDouble(v[3]), "necesidad", fuente)))
                    .toList().stream();
        }
    }

    private static double cobertura(Hallazgo h) {
        if (h == null) return 0;
        return switch (h.declaracion()) {
            case ACTUAL -> 1;
            case CON_APOYO -> .5;
            default -> 0;
        };
    }

    private static String clasificar(double esperado, double actual) {
        if (actual > esperado) return "FP";
        if (actual < esperado) return "FN";
        return esperado > 0 ? "VP" : "VN";
    }

    private Map<String, Object> registrar(Caso c, Hallazgo h, double actual) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", c.id()); r.put("concepto", c.concepto()); r.put("dominio", c.dominio());
        r.put("texto", c.texto()); r.put("fuente", c.fuente()); r.put("grupo", c.grupo());
        r.put("esperado", c.esperado()); r.put("actual", actual);
        r.put("declaracion", h == null ? "AUSENTE" : h.declaracion());
        r.put("resultado", clasificar(c.esperado(), actual));
        RESULTADOS.add(r);
        return r;
    }

    private PerfilNino ninoConNecesidad(Caso c) {
        var n = PerfilesCompatibilidad.nino();
        try {
            n.setCondicionesSalud(List.of(CondicionSalud.valueOf(c.concepto())));
        } catch (IllegalArgumentException noEsCondicion) {
            if (c.concepto().startsWith("ALERGIA_")) {
                n.setAlergias(List.of(Alergia.valueOf(c.concepto().substring(8))));
            } else if (c.concepto().equals("INTOLERANCIA_LACTOSA")) {
                n.setIntoleranciaLactosa(RespuestaSiNoNoSabe.SI);
            } else n.setNecesidadesApoyo(c.necesidad());
        }
        return n;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("capacidades")
    void comparaLaCoberturaConLaDeclaracionHumanaEsperada(Caso c) {
        var h = analizador.analizarTexto("nota", c.texto(), c.dominio(), false).obtener(c.concepto());
        double actual = cobertura(h);
        var registro = registrar(c, h, actual);
        if (c.dominio() == Dominio.SALUD) {
            var n = ninoConNecesidad(c);
            var f = PerfilesCompatibilidad.familia();
            f.setNotaCondicionesSalud(c.texto());
            var r = motor.candidato(n, f, new PesosScoring());
            registro.put("puntosSalud", ((List<?>) ((Map<?, ?>) r.get("f")).get("salud")).get(0));
            registro.put("estadoSalud", ((Map<?, ?>) r.get("estadosFactores")).get("salud"));
        }
        assertEquals(c.esperado(), actual, c.id() + " " + c.texto());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("necesidades")
    void compruebaLaInclusionDeLaNecesidadSegunSuFuente(Caso c) {
        Analisis a;
        if (c.fuente().equals("perfil")) {
            a = analizador.analizarTexto("necesidadesApoyo", c.texto(), Dominio.SALUD, true);
        } else {
            var e = new EvaluacionPsicologica();
            ReflectionTestUtils.setField(e, "id", 1L);
            e.setNumeroVersion(1);
            e.setRecomendaciones(c.texto());
            a = analizador.necesidades(PerfilesCompatibilidad.nino(), List.of(e));
        }
        var h = a.obtener(c.concepto());
        double actual = h != null && (h.declaracion() == Declaracion.NECESIDAD
                || h.declaracion() == Declaracion.AMBIGUA) ? 1 : 0;
        registrar(c, h, actual).put("necesidadesEnDenominador", a.necesidadesSalud().size());
        assertEquals(c.esperado(), actual, c.id() + " " + c.texto());
    }

    @AfterAll
    static void escribirResultadosEInventario() throws Exception {
        Path destino = Path.of("target/cobertura-linguistica");
        Files.createDirectories(destino);
        var json = new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter();
        List<Map<String, Object>> resultados = RESULTADOS.stream()
                .sorted(Comparator.comparing(r -> r.get("fuente") + ":" + r.get("id"))).toList();
        json.writeValue(destino.resolve("resultados.json").toFile(), resultados);
        Object vocabulario = ReflectionTestUtils.getField(AnalizadorTextoCompatibilidad.class, "VOCABULARIO");
        List<Map<String, Object>> inventario = new ArrayList<>();
        for (var entrada : ((Map<?, ?>) vocabulario).entrySet()) {
            var concepto = entrada.getValue();
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", entrada.getKey());
            for (String campo : List.of("nombre", "dominio", "expresion")) {
                Method metodo = concepto.getClass().getDeclaredMethod(campo);
                metodo.setAccessible(true);
                Object valor = metodo.invoke(concepto);
                fila.put(campo, valor instanceof Pattern patron ? patron.pattern() : valor.toString());
            }
            inventario.add(fila);
        }
        json.writeValue(destino.resolve("inventario.json").toFile(), inventario);
        Map<String, Object> patrones = new TreeMap<>();
        for (var campo : AnalizadorTextoCompatibilidad.class.getDeclaredFields()) {
            if (campo.getType() != Pattern.class) continue;
            campo.setAccessible(true);
            var patron = (Pattern) campo.get(null);
            patrones.put(campo.getName(), Map.of("expresion", patron.pattern(), "flags", patron.flags()));
        }
        json.writeValue(destino.resolve("patrones.json").toFile(), patrones);
        json.writeValue(destino.resolve("resumen.json").toFile(), resultados.stream()
                .collect(Collectors.groupingBy(r -> r.get("fuente").toString(), TreeMap::new,
                        Collectors.groupingBy(r -> r.get("resultado").toString(), TreeMap::new, Collectors.counting()))));
    }
}
