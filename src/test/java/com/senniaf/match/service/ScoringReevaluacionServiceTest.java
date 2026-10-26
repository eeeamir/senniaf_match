package com.senniaf.match.service;

import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.repository.DecisionComiteRepository;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import com.senniaf.match.repository.PerfilNinoRepository;
import com.senniaf.match.repository.PesosScoringRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScoringReevaluacionServiceTest {

    @Test
    void solicitudDelComiteMarcaPendienteSinSumarUnaReevaluacionCompletada() {
        PerfilNinoRepository ninos = mock(PerfilNinoRepository.class);
        PerfilFamiliaRepository familias = mock(PerfilFamiliaRepository.class);
        DecisionComiteRepository decisiones = mock(DecisionComiteRepository.class);
        PesosScoringRepository pesos = mock(PesosScoringRepository.class);

        PerfilNino nino = new PerfilNino();
        nino.setCodigoCaso("NINO-001");
        nino.setPerfilCompleto(true);
        PerfilFamilia familia = new PerfilFamilia();
        familia.setCodigoCaso("FAM-001");
        familia.setPerfilCompleto(true);
        when(ninos.findByCodigoCaso("NINO-001")).thenReturn(Optional.of(nino));
        when(familias.findByCodigoCaso("FAM-001")).thenReturn(Optional.of(familia));

        ScoringService servicio = new ScoringService(ninos, familias, decisiones, pesos);
        Map<String, Object> resultado = servicio.decidir("NINO-001", "FAM-001", "REEVALUACION",
                "Se necesita una entrevista familiar actualizada", "CM-001");

        assertTrue(familia.isReevaluacionPendiente());
        assertEquals(0, familia.getNumeroReevaluaciones());
        assertEquals(0, resultado.get("numeroReevaluaciones"));
        assertEquals(false, resultado.get("alertaRevisionTecnica"));
        verify(familias).save(familia);
        verify(decisiones).save(any());
    }
}
