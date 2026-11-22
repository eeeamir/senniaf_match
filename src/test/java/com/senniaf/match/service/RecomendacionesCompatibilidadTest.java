package com.senniaf.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecomendacionesCompatibilidadTest {
    private final PerfilNinoRepository ninos = mock(PerfilNinoRepository.class);
    private final PerfilFamiliaRepository familias = mock(PerfilFamiliaRepository.class);
    private final EvaluacionPsicologicaRepository informes = mock(EvaluacionPsicologicaRepository.class);
    private final ScoringService motor = new ScoringService(ninos, familias,
            mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));
    private PerfilNino n;
    private PerfilFamilia f;

    @BeforeEach
    void preparar() {
        n = PerfilesCompatibilidad.nino();
        f = PerfilesCompatibilidad.familia();
        ReflectionTestUtils.setField(motor, "evaluaciones", informes);
        f.setNotaCondicionesSalud("Podemos acompañar las sesiones psicológicas. Podemos mantener horarios constantes");
    }

    private EvaluacionPsicologica informe(long id, String recomendacion) {
        var e = new EvaluacionPsicologica();
        ReflectionTestUtils.setField(e, "id", id);
        e.setExpediente(n);
        e.setNumeroVersion(1);
        e.setRecomendaciones(recomendacion);
        e.setEvaluacion("CONTENIDO PRIVADO DE LA EVALUACIÓN");
        e.setObservaciones("Necesita supervisión permanente");
        e.setHallazgosProfesionales("Necesita apoyo educativo especializado");
        return e;
    }

    private Map<String, Object> calcular(EvaluacionPsicologica... evaluaciones) {
        when(informes.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso()))
                .thenReturn(List.of(evaluaciones));
        return motor.candidato(n, f, new PesosScoring());
    }

    private int salud(Map<String, Object> r) {
        return (Integer) ((List<?>) ((Map<?, ?>) r.get("f")).get("salud")).get(0);
    }

    @Test
    void unaRecomendacionConcretaAportaPuntosSinExponerElInforme() throws Exception {
        var e = informe(1, "Requiere continuar sus sesiones psicológicas. DATO PRIVADO SIN NECESIDAD COMPARABLE");
        var r = calcular(e);
        assertEquals(35, salud(r));
        String json = new ObjectMapper().writeValueAsString(r);
        assertFalse(json.contains("DATO PRIVADO"));
        assertFalse(json.contains("CONTENIDO PRIVADO"));
        assertFalse(json.contains("Requiere continuar"));
        assertTrue(json.contains("recomendacionesPsicologicas:1:v1"));
    }

    @Test
    void unaCorreccionEliminaLaNecesidadSustituidaSinInvalidarOtrosInformes() {
        var anterior = informe(1, "Requiere acompañamiento psicológico");
        var corregida = informe(2, "No requiere acompañamiento psicológico");
        corregida.setRevisionDe(anterior);
        corregida.setNumeroVersion(2);
        assertEquals(0, salud(calcular(corregida, anterior)));
        var independiente = informe(3, "Requiere horarios consistentes");
        assertEquals(35, salud(calcular(independiente, corregida, anterior)));
    }

    @Test
    void informesIndependientesContradictoriosNoEligenLaMayorCobertura() {
        var positivo = informe(1, "Requiere acompañamiento psicológico");
        var negativo = informe(2, "No requiere acompañamiento psicológico");
        var r = calcular(negativo, positivo);
        assertEquals(0, salud(r));
        assertEquals("DESCONOCIDO", ((Map<?, ?>) r.get("estadosFactores")).get("salud"));
        var inverso = calcular(positivo, negativo);
        for (String campo : List.of("puntaje", "f", "estadosFactores", "maximoAplicable", "pesoEvaluado")) {
            assertEquals(r.get(campo), inverso.get(campo));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Se descartó la necesidad de terapia psicológica",
            "Se recomienda valorar si necesita terapia psicológica", "Antecedentes de terapia psicológica",
            "Su hermana requiere terapia psicológica", "Es introvertido", "Posiblemente necesita terapia psicológica"})
    void noActivaHipotesisDescartesPersonalidadNiNecesidadesDeTerceros(String texto) {
        assertEquals(0, salud(calcular(informe(1, texto))));
    }

    @Test
    void deduplicaLaMismaNecesidadEntrePerfilYVariasRecomendaciones() {
        n.setNecesidadesApoyo("Requiere acompañamiento psicológico");
        n.setCondicionesSaludDetalle("Necesita seguimiento psicológico");
        var r = calcular(informe(1, "Continuar terapia psicológica"), informe(2, "Requiere continuar sus sesiones de psicología"));
        assertEquals(35, salud(r));
        var analisis = (AnalizadorTextoCompatibilidad.Analisis) ((Map<?, ?>) r.get("evidenciasCompatibilidad")).get("necesidades");
        assertEquals(1, analisis.necesidadesSalud().size());
        assertEquals(4, analisis.obtener("ACOMPANAMIENTO_PSICOLOGICO").evidencias().size());
    }

    @Test
    void noConsultaInformesPorCadaFamiliaCandidata() {
        var segunda = PerfilesCompatibilidad.familia();
        segunda.setCodigoCaso("PRUEBA-F2");
        when(ninos.findByCodigoCaso(n.getCodigoCaso())).thenReturn(Optional.of(n));
        when(familias.findAll()).thenReturn(List.of(f, segunda));
        when(informes.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso()))
                .thenReturn(List.of(informe(1, "Requiere acompañamiento psicológico")));
        motor.caso(n.getCodigoCaso());
        verify(informes, times(1)).findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso());
        verify(informes, never()).save(any());
    }

    @Test
    void cadenaDeCorreccionesConRecomendacionVaciaNoMantieneNecesidadesAnteriores() {
        var a = informe(1, "Requiere acompañamiento psicológico");
        var b = informe(2, "Requiere horarios estables");
        b.setRevisionDe(a);
        var c = informe(3, "");
        c.setRevisionDe(b);
        assertEquals(0, salud(calcular(c, b, a)));
    }

    @Test
    void recomendacionConcretaSinEquivalenciaNoDesapareceDelDenominador() {
        assertEquals(18, salud(calcular(informe(1,
                "Requiere acompañamiento psicológico. Requiere acompañamiento especializado"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Se recomienda acompañamiento psicológico", "Continuar terapia psicológica",
            "Sí requiere seguimiento psicológico"})
    void recomendacionesConcretasEquivalentes(String texto) {
        assertEquals(35, salud(calcular(informe(1, texto))));
    }

    @Test
    void recomendacionCondicionalNoSeConvierteEnUnaNecesidadConfirmada() {
        assertEquals(0, salud(calcular(informe(1, "Requiere terapia psicológica si presenta dificultades"))));
    }

    @Test
    void unaHipotesisPosteriorNoDescartaOtraNecesidadConfirmadaEnLaMismaOracion() {
        assertEquals(35, salud(calcular(informe(1,
                "Requiere horarios estables y valorar si necesita terapia psicológica"))));
    }

    @Test
    void unaPreguntaNoConfirmaUnaNecesidadProfesional() {
        assertEquals(0, salud(calcular(informe(1, "¿Requiere terapia psicológica?"))));
    }
}
