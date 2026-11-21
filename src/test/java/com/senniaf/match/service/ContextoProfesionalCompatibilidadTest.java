package com.senniaf.match.service;

import com.senniaf.match.model.EvaluacionPsicologica;
import com.senniaf.match.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextoProfesionalCompatibilidadTest {
    @Test
    void lasCorreccionesConservanLaVersionAnteriorYNoAcumulanNecesidadesEnScoring() {
        var n = PerfilesCompatibilidad.nino();
        var repositorio = mock(PerfilNinoRepository.class);
        var evaluaciones = mock(EvaluacionPsicologicaRepository.class);
        var anterior = new EvaluacionPsicologica();
        ReflectionTestUtils.setField(anterior, "id", 1L);
        anterior.setExpediente(n);
        anterior.setRecomendaciones("Se recomienda valorar acompañamiento psicológico");
        anterior.setHallazgosProfesionales("Hipótesis pendiente de evaluación");
        var corregida = new EvaluacionPsicologica();
        ReflectionTestUtils.setField(corregida, "id", 2L);
        corregida.setExpediente(n);
        corregida.setRevisionDe(anterior);
        corregida.setNumeroVersion(2);
        corregida.setRecomendaciones("No requiere acompañamiento psicológico");
        when(repositorio.findByCodigoCaso(n.getCodigoCaso())).thenReturn(Optional.of(n));
        when(evaluaciones.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso()))
                .thenReturn(List.of(corregida, anterior));
        var perfiles = new PerfilNinoService(repositorio, null, evaluaciones, null);

        var informes = perfiles.consultarEvaluaciones(n.getCodigoCaso(), "COMITE-PRUEBA", "COMITE");

        assertTrue(informes.get(0).vigente());
        assertEquals(2, informes.get(0).version());
        assertFalse(informes.get(1).vigente());
        assertEquals(anterior.getRecomendaciones(), informes.get(1).recomendaciones());
        var motor = new ScoringService(repositorio, mock(PerfilFamiliaRepository.class),
                mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));
        var resultado = motor.candidato(n, PerfilesCompatibilidad.familia(), new com.senniaf.match.model.PesosScoring());
        assertEquals("NO_APLICA", ((Map<?, ?>) resultado.get("estadosFactores")).get("salud"));
        verify(evaluaciones, never()).save(any());
    }
}
