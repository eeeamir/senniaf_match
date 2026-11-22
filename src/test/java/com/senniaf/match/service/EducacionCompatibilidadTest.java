package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EducacionCompatibilidadTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Requiere apoyo educativo individualizado|Podemos ofrecer acompañamiento educativo individualizado|35",
        "Necesita ayuda específica con sus tareas|Podemos proporcionar apoyo con las tareas|35",
        "Requiere seguimiento de sus actividades escolares|Podemos realizar seguimiento educativo|35",
        "Necesita acompañamiento de refuerzo escolar|Podemos facilitar el refuerzo escolar correspondiente|35",
        "Requiere apoyo educativo especializado|Podemos ayudarlo con las tareas|0",
        "Requiere apoyo educativo individualizado|Podemos proporcionar apoyo con las tareas|0",
        "Requiere apoyo educativo individualizado|Podemos ofrecer apoyo educativo individualizado con apoyo|18",
        "Requiere apoyo educativo individualizado|No podemos ofrecer apoyo educativo individualizado|0",
        "Tiene buen rendimiento académico|Podemos ofrecer apoyo educativo individualizado|0"
    })
    void distingueNecesidadesEducativasSinPuntuarRendimiento(String necesidad, String capacidad, int esperado) {
        var n = PerfilesCompatibilidad.nino();
        var f = PerfilesCompatibilidad.familia();
        n.setNecesidadesApoyo(necesidad);
        f.setNotaCondicionesSalud(capacidad);
        var motor = new ScoringService(mock(PerfilNinoRepository.class), mock(PerfilFamiliaRepository.class),
                mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));
        var r = motor.candidato(n, f, new PesosScoring());
        assertEquals(esperado, ((List<?>) ((Map<?, ?>) r.get("f")).get("salud")).get(0));
    }
}
