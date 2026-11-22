package com.senniaf.match.service;

import com.senniaf.match.service.AnalizadorTextoCompatibilidad.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ApoyoCotidianoDeclaradoTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Contamos con una persona de apoyo que puede acompañarlo en actividades cotidianas|ACTUAL",
        "Contamos con una persona de apoyo que no puede acompañarlo en actividades cotidianas|NEGATIVA",
        "Contamos con una persona de apoyo que podría acompañarlo en actividades cotidianas|AMBIGUA"
    })
    void noInfiereLaCapacidadEspecializadaSoloPorDisponerDeUnaPersona(String nota, Declaracion esperado) {
        var n = PerfilesCompatibilidad.nino();
        n.setNecesidadesApoyo("Requiere acompañamiento en actividades cotidianas");
        var f = PerfilesCompatibilidad.familia();
        f.setNotaRedApoyo(nota);
        var a = new AnalizadorTextoCompatibilidad();
        assertEquals(Declaracion.NECESIDAD, a.necesidades(n).obtener("CUIDADOS_DIARIOS").declaracion());
        assertEquals(esperado, a.capacidades(f).obtener("CUIDADOS_DIARIOS").declaracion());
    }
}
