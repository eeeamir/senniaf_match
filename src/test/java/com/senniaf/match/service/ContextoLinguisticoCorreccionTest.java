package com.senniaf.match.service;

import com.senniaf.match.model.EvaluacionPsicologica;
import com.senniaf.match.service.AnalizadorTextoCompatibilidad.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class ContextoLinguisticoCorreccionTest {
    private final AnalizadorTextoCompatibilidad analizador = new AnalizadorTextoCompatibilidad();

    @ParameterizedTest
    @ValueSource(strings = {
        "Antes acompañamos asma, pero ahora podemos acompañarlo",
        "En el pasado acompañamos asma y ahora podemos acompañar asma",
        "Antes acompañamos asma. Actualmente podemos acompañar asma"
    })
    void noPierdeLaDeclaracionActualAlMencionarExperienciaPasada(String texto) {
        assertEquals(Declaracion.ACTUAL, capacidad(texto, "ASMA"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Podemos acompañar asma y no siempre atender diabetes|ACTUAL|AMBIGUA",
        "Podemos acompañar asma y nunca atender diabetes|ACTUAL|NEGATIVA",
        "No siempre podemos acompañar asma, pero podemos atender diabetes|AMBIGUA|ACTUAL",
        "Antes acompañamos asma, pero actualmente podemos atender diabetes|INSUFICIENTE|ACTUAL"
    })
    void conservaElAmbitoDeLasAccionesIndependientes(String texto, Declaracion asma, Declaracion diabetes) {
        assertEquals(asma, capacidad(texto, "ASMA"));
        assertEquals(diabetes, capacidad(texto, "DIABETES"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Requiere terapia psicológica?", "¿Requiere terapia psicológica?",
        "Se descartó la necesidad de terapia psicológica", "Antecedentes de terapia psicológica",
        "Requiere terapia psicológica si presenta dificultades", "Posiblemente requiere terapia psicológica"
    })
    void aplicaElMismoFiltroDeNecesidadesActivasEnAmbasFuentes(String texto) {
        assertTrue(analizador.analizarTexto("necesidadesApoyo", texto, Dominio.SALUD, true)
                .necesidadesSalud().isEmpty());
        assertTrue(analizador.necesidades(PerfilesCompatibilidad.nino(), List.of(informe(texto)))
                .necesidadesSalud().isEmpty());
    }

    @Test
    void hemodialisisConservaUnaNecesidadPendienteSinInventarUnaEquivalencia() {
        for (var a : List.of(analizador.analizarTexto("necesidadesApoyo", "Requiere hemodiálisis", Dominio.SALUD, true),
                analizador.necesidades(PerfilesCompatibilidad.nino(), List.of(informe("Requiere hemodiálisis"))))) {
            assertEquals(1, a.necesidadesSalud().size());
            assertEquals("NECESIDAD_SIN_PRECISAR", a.necesidadesSalud().get(0).concepto());
            assertEquals(Declaracion.AMBIGUA, a.necesidadesSalud().get(0).declaracion());
        }
        assertNull(capacidad("Podemos acompañar hemodiálisis", "NECESIDAD_SIN_PRECISAR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"¿Requiere hemodiálisis?", "Es introvertido", "Le gusta la rutina fija",
            "Antecedentes de hemodiálisis", "Se descartó la necesidad de hemodiálisis"})
    void noIntroduceNecesidadesPendientesPorComentariosOContextosInactivos(String texto) {
        assertTrue(analizador.analizarTexto("necesidadesApoyo", texto, Dominio.SALUD, true)
                .necesidadesSalud().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Podemos realizar tratamiento psicológico", "Podemos dar terapia psicológica",
            "Podemos ofrecer atención psicológica"})
    void prestarAtencionClinicaNoEquivaleADeclararAcompanamiento(String texto) {
        assertNotEquals(Declaracion.ACTUAL, capacidad(texto, "ACOMPANAMIENTO_PSICOLOGICO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Podemos ofrecer acompañamiento psicológico",
            "Podemos brindar acompañamiento psicológico", "Podemos realizar traslados a sus citas con el psicólogo"})
    void distingueLaPrestacionClinicaDelAcompanamientoFamiliar(String texto) {
        assertEquals(Declaracion.ACTUAL, capacidad(texto, "ACOMPANAMIENTO_PSICOLOGICO"));
    }

    @Test
    void conservaLaEspecificidadDeLosApoyos() {
        assertNull(capacidad("Podemos vigilarlo todos los días", "SUPERVISION_PERMANENTE"));
        assertNull(capacidad("Podemos ayudarlo a hacer sus tareas", "APOYO_EDUCATIVO_ESPECIALIZADO"));
        assertNull(capacidad("Podemos apoyarlo emocionalmente", "ACOMPANAMIENTO_PSICOLOGICO"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "No estamos disponibles para llevarlo a sus citas con el psicólogo|NEGATIVA",
        "Podemos facilitar su atención psicológica si recibimos capacitación|CON_APOYO",
        "No siempre podemos acompañarlo durante su tratamiento psicológico|AMBIGUA"
    })
    void lasEquivalenciasNuevasRespetanLasLimitaciones(String texto, Declaracion esperado) {
        assertEquals(esperado, capacidad(texto, "ACOMPANAMIENTO_PSICOLOGICO"));
    }

    private Declaracion capacidad(String texto, String concepto) {
        var h = analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false).obtener(concepto);
        return h == null ? null : h.declaracion();
    }

    private EvaluacionPsicologica informe(String texto) {
        var e = new EvaluacionPsicologica();
        ReflectionTestUtils.setField(e, "id", 1L);
        e.setNumeroVersion(1);
        e.setRecomendaciones(texto);
        return e;
    }
}
