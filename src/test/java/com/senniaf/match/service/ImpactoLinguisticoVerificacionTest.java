package com.senniaf.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImpactoLinguisticoVerificacionTest {
    private static final List<Map<String, Object>> RESULTADOS = new ArrayList<>();
    private final ScoringService motor = new ScoringService(mock(PerfilNinoRepository.class),
            mock(PerfilFamiliaRepository.class), mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "I01|false|Requiere acompañamiento psicológico|Podemos acompañarlo a terapia psicológica|35",
        "I02|false|Requiere acompañamiento psicológico|Podemos llevarlo a sus citas con el psicólogo|35",
        "I03|false|Requiere acompañamiento psicológico|Podemos facilitar que continúe su atención psicológica|35",
        "I04|true|Necesita que lo lleven a sus citas con el psicólogo|sin capacidad adicional|18",
        "I05|true|Requiere tratamiento especializado|sin capacidad adicional|18",
        "I06|true|Requiere hemodiálisis|sin capacidad adicional|18",
        "I07|false|¿Requiere terapia psicológica?|Podemos acompañar terapia psicológica|0",
        "I08|true|Sin otra necesidad|No siempre podemos acompañar asma|0",
        "I09|true|Sin otra necesidad|Antes acompañamos a un niño con asma|0",
        "I10|true|Es introvertido|Podemos acompañar asma|35",
        "I11|false|Requiere acompañamiento psicológico|No podemos acompañar terapia psicológica|0",
        "I12|false|Requiere acompañamiento psicológico|Quizás podríamos acompañar terapia psicológica|0"
    })
    void contrastaElPuntajeRealConElSignificadoEsperado(String id, boolean asma, String necesidad,
                                                       String capacidad, int esperado) {
        var n = PerfilesCompatibilidad.nino();
        var f = PerfilesCompatibilidad.familia();
        if (asma) {
            n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
            if (!id.equals("I08") && !id.equals("I09")) f.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA));
        }
        n.setNecesidadesApoyo(necesidad);
        f.setNotaCondicionesSalud(capacidad);
        var r = motor.candidato(n, f, new PesosScoring());
        int actual = (Integer) ((List<?>) ((Map<?, ?>) r.get("f")).get("salud")).get(0);
        var fila = new LinkedHashMap<String, Object>();
        fila.put("id", id); fila.put("asmaEstructurada", asma); fila.put("necesidad", necesidad);
        fila.put("capacidad", capacidad); fila.put("pesoSalud", 35); fila.put("esperado", esperado);
        fila.put("actual", actual); fila.put("totalReal", r.get("puntaje"));
        fila.put("estado", ((Map<?, ?>) r.get("estadosFactores")).get("salud"));
        fila.put("razon", ((List<?>) ((Map<?, ?>) r.get("f")).get("salud")).get(1));
        RESULTADOS.add(fila);
        assertEquals(esperado, actual, id);
    }

    @AfterAll
    static void guardarResultados() throws Exception {
        Path destino = Path.of("target/cobertura-linguistica");
        Files.createDirectories(destino);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(destino.resolve("impacto.json").toFile(), RESULTADOS);
    }
}
