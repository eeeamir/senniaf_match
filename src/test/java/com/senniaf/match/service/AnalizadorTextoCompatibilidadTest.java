package com.senniaf.match.service;

import com.senniaf.match.service.AnalizadorTextoCompatibilidad.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AnalizadorTextoCompatibilidadTest {
    private final AnalizadorTextoCompatibilidad analizador = new AnalizadorTextoCompatibilidad();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Puedo acompañar asma|ACTUAL",
        "PODEMOS   ACOMPAÑAR ASMA|ACTUAL",
        "No puedo acompañar asma|NEGATIVA",
        "Nunca puedo atender asma|NEGATIVA",
        "Tampoco podemos acompañar asma|NEGATIVA",
        "No tengo experiencia con el asma, pero puedo acompañarlo|ACTUAL",
        "No tenemos dificultades para acompañar asma|ACTUAL",
        "No solo podemos acompañar asma, también tenemos apoyo|ACTUAL",
        "Mi hermana tiene asma|INSUFICIENTE",
        "Mi hermana no puede acompañar asma|INSUFICIENTE",
        "Podemos acompañar asma con apoyo adicional|CON_APOYO",
        "Podemos acompañar asma si recibimos capacitación|CON_APOYO",
        "Si contamos con apoyo, podemos acompañar asma|CON_APOYO",
        "Podemos acompañar asma si no contamos con apoyo|AMBIGUA",
        "Podemos acompañar asma si jamás contamos con apoyo|AMBIGUA",
        "Quizás podríamos acompañar asma|AMBIGUA",
        "Lo acompañaríamos por el asma si fuera posible|AMBIGUA",
        "Puedo acompañar asma. No puedo acompañar asma|AMBIGUA",
        "No puedo acompañar asma. Puedo acompañar asma|AMBIGUA",
        "Sí, puedo acompañar asma|ACTUAL"
    })
    void interpretaLaAccionYNoUnaPalabraAislada(String texto, Declaracion esperado) {
        var h = analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false).obtener("ASMA");
        assertNotNull(h);
        assertEquals(esperado, h.declaracion());
        assertEquals("notaCondicionesSalud", h.evidencias().get(0).campo());
        assertEquals(Origen.TEXTO, h.evidencias().get(0).origen());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Podemos acompañar terapia de lenguaje", "Podemos acompañar las terapias del lenguaje",
        "Nuestra familia puede acompañar las terapias de lenguaje del niño"
    })
    void equivalenciasLenguaje(String texto) {
        var h = analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false)
                .obtener("TERAPIA_LENGUAJE");
        assertNotNull(h);
        assertEquals(Declaracion.ACTUAL, h.declaracion());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Mi hermana no nos ayuda con los cuidados diarios|NEGATIVA",
        "Mi hermana no puede ayudarnos|NEGATIVA",
        "No contamos con apoyo familiar|NEGATIVA",
        "Nos ayudan con los cuidados diarios si pueden|AMBIGUA"
    })
    void apoyoNegadoOIncierto(String texto, Declaracion esperado) {
        var r = analizador.analizarTexto("notaRedApoyo", texto, Dominio.RED, false);
        assertTrue(r.conceptos().values().stream().anyMatch(h -> h.declaracion() == esperado));
    }

    @Test
    void respetaLimitesDePalabraYNoConfundeTerapias() {
        var r = analizador.analizarTexto("notaCondicionesSalud",
                "Podemos acompañar terapia ocupacional. Fantasma, tema y otra", Dominio.SALUD, false);
        assertNull(r.obtener("ASMA"));
        assertNull(r.obtener("TEA"));
        assertNull(r.obtener("TERAPIA_LENGUAJE"));
        assertEquals(Declaracion.ACTUAL, r.obtener("TERAPIA_OCUPACIONAL").declaracion());
    }

    @Test
    void noTransfiereLaExperienciaDeUnTerceroPorAnafora() {
        var r = analizador.analizarTexto("notaCondicionesSalud",
                "Mi hermana tiene asma, pero puedo acompañarlo", Dominio.SALUD, false);
        assertEquals(Declaracion.INSUFICIENTE, r.obtener("ASMA").declaracion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Podemos facilitar una sesión psicológica",
            "Podemos acompañar sesiones psicológicas", "Podemos acompañar terapia psicológica"})
    void equivalenciasDeAcompanamientoPsicologico(String texto) {
        var r = analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false);
        assertEquals(Declaracion.ACTUAL, r.obtener("ACOMPANAMIENTO_PSICOLOGICO").declaracion());
    }

    @Test
    void negacionDeLaSegundaAccionNoSeConvierteEnCapacidad() {
        var r = analizador.analizarTexto("notaCondicionesSalud",
                "Podemos acompañar asma y no atender diabetes", Dominio.SALUD, false);
        assertEquals(Declaracion.ACTUAL, r.obtener("ASMA").declaracion());
        assertEquals(Declaracion.NEGATIVA, r.obtener("DIABETES").declaracion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mi hermano recibe terapia de lenguaje", "Nuestra hija asistió a terapia de lenguaje"})
    void experienciasDeTercerosNoSonCapacidades(String texto) {
        var r = analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false);
        assertEquals(Declaracion.INSUFICIENTE, r.obtener("TERAPIA_LENGUAJE").declaracion());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Podemos acompañar asma y atender diabetes con apoyo|ACTUAL|CON_APOYO",
        "Podemos acompañar asma con apoyo y atender diabetes|CON_APOYO|ACTUAL",
        "Podemos acompañar asma y no atender diabetes|ACTUAL|NEGATIVA"
    })
    void laCondicionSeAplicaALaAccionCorrespondiente(String texto, Declaracion asma, Declaracion diabetes) {
        var r = analizador.analizarTexto("notaCondicionesSalud", texto, Dominio.SALUD, false);
        assertEquals(asma, r.obtener("ASMA").declaracion());
        assertEquals(diabetes, r.obtener("DIABETES").declaracion());
    }
}
