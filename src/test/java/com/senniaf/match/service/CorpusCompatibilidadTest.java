package com.senniaf.match.service;

import com.senniaf.match.service.AnalizadorTextoCompatibilidad.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class CorpusCompatibilidadTest {
    private final AnalizadorTextoCompatibilidad analizador = new AnalizadorTextoCompatibilidad();

    static Stream<Arguments> conceptos() {
        return Stream.of(
                Arguments.of("ASMA", "asma"), Arguments.of("DIABETES", "diabetes"),
                Arguments.of("EPILEPSIA", "epilepsia"), Arguments.of("TDAH", "TDAH"), Arguments.of("TEA", "TEA"),
                Arguments.of("DISCAPACIDAD_FISICA", "discapacidad física"),
                Arguments.of("DISCAPACIDAD_INTELECTUAL", "discapacidad intelectual"),
                Arguments.of("DISCAPACIDAD_VISUAL", "discapacidad visual"),
                Arguments.of("DISCAPACIDAD_AUDITIVA", "discapacidad auditiva"),
                Arguments.of("INTOLERANCIA_LACTOSA", "intolerancia a la lactosa"),
                Arguments.of("ALERGIA_ALIMENTARIA", "alergia alimentaria"),
                Arguments.of("ALERGIA_MEDICAMENTOS", "alergia a medicamentos"),
                Arguments.of("ALERGIA_AMBIENTAL_POLEN", "alergia al polen"),
                Arguments.of("ALERGIA_PICADURA_INSECTOS", "alergia a picaduras de insectos"),
                Arguments.of("TERAPIA_LENGUAJE", "terapia del lenguaje"),
                Arguments.of("FISIOTERAPIA", "fisioterapia"), Arguments.of("TERAPIA_OCUPACIONAL", "terapia ocupacional"),
                Arguments.of("ACOMPANAMIENTO_PSICOLOGICO", "acompañamiento psicológico"),
                Arguments.of("RUTINA_ESTABLE", "rutinas estables"),
                Arguments.of("ACOMPANAMIENTO_EMOCIONAL", "apoyo emocional"),
                Arguments.of("APOYO_ADAPTACION", "apoyo durante su adaptación"),
                Arguments.of("SUPERVISION_COTIDIANA", "supervisión cotidiana"),
                Arguments.of("SUPERVISION_PERMANENTE", "supervisión permanente"),
                Arguments.of("APOYO_EDUCATIVO_INDIVIDUALIZADO", "apoyo educativo individualizado"),
                Arguments.of("APOYO_EDUCATIVO_ESPECIALIZADO", "apoyo educativo especializado"),
                Arguments.of("AYUDA_TAREAS", "ayuda con las tareas"),
                Arguments.of("SEGUIMIENTO_ESCOLAR", "seguimiento educativo"),
                Arguments.of("REFUERZO_ESCOLAR", "refuerzo escolar"));
    }

    private Hallazgo capacidad(String concepto, String texto) {
        return analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false).obtener(concepto);
    }

    @ParameterizedTest
    @MethodSource("conceptos")
    void afirmacionCondicionNegacionAmbiguedadYTerceroPorConcepto(String concepto, String expresion) {
        String base = "Podemos acompañar " + expresion;
        assertEquals(Declaracion.ACTUAL, capacidad(concepto, base).declaracion());
        assertEquals(Declaracion.CON_APOYO, capacidad(concepto, base + " con apoyo").declaracion());
        assertEquals(Declaracion.NEGATIVA, capacidad(concepto, "No " + base).declaracion());
        assertEquals(Declaracion.AMBIGUA, capacidad(concepto, "Quizás " + base).declaracion());
        assertEquals(Declaracion.INSUFICIENTE, capacidad(concepto, "Mi hermana dice: " + base).declaracion());
        var n = analizador.analizarTexto("necesidadesApoyo", "Requiere acompañamiento para " + expresion,
                Dominio.SALUD, true);
        assertEquals(Declaracion.NECESIDAD, n.obtener(concepto).declaracion());
        assertNull(capacidad(concepto, "Somos responsables y cariñosos"));
    }

    @ParameterizedTest
    @MethodSource("conceptos")
    void repeticionesNormalizacionContradiccionesYOrdenPorConcepto(String concepto, String expresion) {
        String capacidad = "Podemos acompañar " + expresion;
        var normalizado = capacidad(concepto, AnalizadorTextoCompatibilidad.normalizar(capacidad).toUpperCase(Locale.ROOT));
        assertEquals(Declaracion.ACTUAL, normalizado.declaracion());
        var repetido = analizador.analizarTexto("notaCondicionesSalud", (capacidad + ". ").repeat(5), Dominio.SALUD, false);
        assertEquals(1, repetido.obtener(concepto).evidencias().size());
        assertEquals(Declaracion.AMBIGUA, capacidad(concepto, capacidad + ". No " + capacidad).declaracion());
        assertEquals(Declaracion.AMBIGUA, capacidad(concepto, "No " + capacidad + ". " + capacidad).declaracion());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ACOMPANAMIENTO_PSICOLOGICO|Podemos acompañarlo a sus sesiones de psicología",
        "ACOMPANAMIENTO_PSICOLOGICO|Tenemos disponibilidad para asistir con el niño a terapia psicológica",
        "ACOMPANAMIENTO_PSICOLOGICO|Podemos facilitar su seguimiento psicológico",
        "RUTINA_ESTABLE|Podemos mantener horarios consistentes",
        "DISCAPACIDAD_VISUAL|Podemos acompañar discapacidades visuales",
        "DISCAPACIDAD_INTELECTUAL|Podemos acompañar discapacidades intelectuales"
    })
    void equivalenciasClarasAdicionales(String concepto, String texto) {
        assertEquals(Declaracion.ACTUAL, capacidad(concepto, texto).declaracion());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "SUPERVISION_PERMANENTE|Podemos acompañarlo a diario",
        "ACOMPANAMIENTO_PSICOLOGICO|Podemos ofrecer apoyo emocional",
        "APOYO_EDUCATIVO_ESPECIALIZADO|Podemos ayudar con las tareas",
        "TERAPIA_LENGUAJE|Podemos acompañar terapia ocupacional"
    })
    void conceptosRelacionadosNoSonEquivalentes(String concepto, String texto) {
        assertNull(capacidad(concepto, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Podemos acompañar terapias psicológicas y mantener rutinas estables, pero no podemos proporcionar supervisión permanente",
            "No podemos proporcionar supervisión permanente, pero podemos mantener rutinas estables y acompañar terapias psicológicas"})
    void enumeracionesConNegacionParcial(String texto) {
        assertEquals(Declaracion.ACTUAL, capacidad("ACOMPANAMIENTO_PSICOLOGICO", texto).declaracion());
        assertEquals(Declaracion.ACTUAL, capacidad("RUTINA_ESTABLE", texto).declaracion());
        assertEquals(Declaracion.NEGATIVA, capacidad("SUPERVISION_PERMANENTE", texto).declaracion());
    }
}
