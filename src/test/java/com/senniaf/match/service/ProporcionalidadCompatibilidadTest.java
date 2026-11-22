package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProporcionalidadCompatibilidadTest {
    private final ScoringService motor = new ScoringService(mock(PerfilNinoRepository.class),
            mock(PerfilFamiliaRepository.class), mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));

    private Map<String, Object> calcular(String necesidades, String capacidades) {
        var n = PerfilesCompatibilidad.nino();
        n.setNecesidadesApoyo(necesidades);
        var f = PerfilesCompatibilidad.familia();
        f.setNotaCondicionesSalud(capacidades);
        return motor.candidato(n, f, new PesosScoring());
    }

    private int salud(Map<String, Object> resultado) {
        return (Integer) ((List<?>) ((Map<?, ?>) resultado.get("f")).get("salud")).get(0);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Necesita acompañamiento psicológico. Requiere apoyo educativo individualizado|Podemos acompañar sesiones de psicología. Podemos ofrecer apoyo educativo individualizado|35",
        "Requiere terapia ocupacional. Necesita apoyo emocional|Podemos acompañar terapia ocupacional|18",
        "Requiere terapia ocupacional. Necesita apoyo emocional|Podemos acompañar terapia ocupacional. Podemos ofrecer apoyo emocional con apoyo|26",
        "Necesita rutinas estables. Requiere acompañamiento psicológico. Necesita apoyo educativo individualizado|Podemos mantener horarios consistentes. Podemos acompañar sesiones de psicología. Podemos ofrecer acompañamiento educativo individualizado|35",
        "Necesita acompañamiento psicológico. Requiere tratamiento especializado|Podemos acompañar sesiones de psicología|18"
    })
    void combinacionesHeterogeneasAplicanElMismoPromedio(String necesidades, String capacidades, int esperado) {
        assertEquals(esperado, salud(calcular(necesidades, capacidades)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ASMA|Necesita acompañamiento psicológico|Podemos acompañar asma. Podemos acompañar sesiones psicológicas|35",
        "ASMA|Necesita apoyo emocional|Podemos acompañar asma|18",
        "ASMA|Necesita apoyo emocional|Podemos acompañar asma. Podemos ofrecer apoyo emocional con apoyo|26",
        "DIABETES|Necesita rutinas estables|Podemos acompañar diabetes. Podemos mantener horarios consistentes|35"
    })
    void condicionesMedicasYApoyosCompartenElMaximo(CondicionSalud condicion, String necesidad, String capacidad, int esperado) {
        var n = PerfilesCompatibilidad.nino();
        n.setCondicionesSalud(List.of(condicion));
        n.setNecesidadesApoyo(necesidad);
        var f = PerfilesCompatibilidad.familia();
        f.setNotaCondicionesSalud(capacidad);
        assertEquals(esperado, salud(motor.candidato(n, f, new PesosScoring())));
    }

    @Test
    void variasCoberturasPuedenOcultarEnElTotalUnaNecesidadNoCubiertaSinBorrarlaDelDesglose() {
        var n = PerfilesCompatibilidad.nino();
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
        n.setNecesidadesApoyo("Necesita rutinas estables. Requiere apoyo emocional. Necesita acompañamiento psicológico. Requiere ayuda con las tareas");
        var f = PerfilesCompatibilidad.familia();
        f.setNotaCondicionesSalud("Podemos mantener horarios consistentes. Podemos ofrecer apoyo emocional. Podemos acompañar sesiones psicológicas. Podemos proporcionar apoyo con las tareas");
        var r = motor.candidato(n, f, new PesosScoring());
        assertEquals(28, salud(r));
        String razon = (String) ((List<?>) ((Map<?, ?>) r.get("f")).get("salud")).get(1);
        assertTrue(razon.contains("Asma: sin capacidad correspondiente registrada"));
    }
}
