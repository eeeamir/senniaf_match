package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.service.AnalizadorTextoCompatibilidad.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcompanamientoSocialCompatibilidadTest {
    private final AnalizadorTextoCompatibilidad analizador = new AnalizadorTextoCompatibilidad();
    private final ScoringService motor = new ScoringService(mock(PerfilNinoRepository.class),
            mock(PerfilFamiliaRepository.class), mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));
    private PerfilNino n;
    private PerfilFamilia f;
    private PesosScoring p;

    @BeforeEach
    void preparar() {
        n = PerfilesCompatibilidad.nino();
        f = PerfilesCompatibilidad.familia();
        p = new PesosScoring();
    }

    static Stream<Arguments> correspondencias() {
        return Stream.of(
                Arguments.of("RUTINA_ESTABLE", "Necesita mantener una rutina diaria estable", "Podemos mantener horarios constantes"),
                Arguments.of("ACOMPANAMIENTO_EMOCIONAL", "Requiere acompañamiento emocional específico", "Podemos ofrecer apoyo emocional"),
                Arguments.of("APOYO_ADAPTACION", "Necesita apoyo durante su adaptación", "Podemos acompañarlo durante el proceso de adaptación"),
                Arguments.of("SUPERVISION_COTIDIANA", "Requiere supervisión cotidiana", "Podemos proporcionar supervisión y acompañamiento diario"));
    }

    private int puntos(String criterio) {
        return ((Number) ((List<?>) ((Map<?, ?>) motor.candidato(n, f, p).get("f")).get(criterio)).get(0)).intValue();
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void coincidenciaActualModificaElPuntaje(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(necesidad);
        assertEquals(0, puntos("salud"));
        f.setNotaCondicionesSalud(capacidad);
        assertEquals(p.getSalud(), puntos("salud"));
        assertEquals(Declaracion.NECESIDAD, analizador.necesidades(n).obtener(concepto).declaracion());
        assertEquals(Declaracion.ACTUAL, analizador.capacidades(f).obtener(concepto).declaracion());
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void capacidadCondicionadaUsaLaMitad(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(necesidad);
        f.setNotaCondicionesSalud(capacidad + " si recibimos capacitación");
        assertEquals(Math.round(p.getSalud() * .5f), puntos("salud"));
        assertEquals(Declaracion.CON_APOYO, analizador.capacidades(f).obtener(concepto).declaracion());
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void negativasHipotesisYTercerosNoAcreditanCobertura(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(necesidad);
        for (String texto : List.of("No " + capacidad, "Quizás " + capacidad, "Mi hermana dice: " + capacidad)) {
            f.setNotaCondicionesSalud(texto);
            assertEquals(0, puntos("salud"), texto);
        }
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void mantieneProcedenciaYUnificaCamposSinBonificarRed(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(necesidad);
        n.setCondicionesSaludDetalle(necesidad);
        f.setNotaCondicionesSalud(capacidad);
        f.setNotaRedApoyo(capacidad);
        f.setNotaEstructuraHogar(capacidad);
        assertEquals(p.getSalud(), puntos("salud"));
        assertEquals(p.getApoyo(), puntos("apoyo"));
        var hallazgo = analizador.capacidades(f).obtener(concepto);
        assertNotNull(hallazgo);
        assertEquals(Set.of("notaCondicionesSalud", "notaRedApoyo", "notaEstructuraHogar"),
                hallazgo.evidencias().stream().map(Evidencia::campo).collect(Collectors.toSet()));
        assertEquals(1, analizador.necesidades(n).necesidadesSalud().size());
        f.setRedApoyo(List.of(FuenteApoyo.NINGUNA_POR_AHORA));
        assertEquals(0, puntos("apoyo"));
        assertEquals(p.getSalud(), puntos("salud"));
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void equivalenciasYRepeticionesConservanLosPuntos(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(necesidad);
        f.setNotaCondicionesSalud(capacidad);
        int esperado = puntos("salud");
        n.setNecesidadesApoyo((necesidad.toUpperCase(Locale.ROOT) + ". ").repeat(3));
        f.setNotaCondicionesSalud((AnalizadorTextoCompatibilidad.normalizar(capacidad).replace(" ", "   ") + ". ").repeat(3));
        assertEquals(esperado, puntos("salud"));
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void contradiccionEntreNotasNoEligeLaCoberturaMayor(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(necesidad);
        f.setNotaCondicionesSalud(capacidad);
        f.setNotaEstructuraHogar("No " + capacidad);
        assertEquals(0, puntos("salud"));
        assertEquals(Declaracion.AMBIGUA, analizador.capacidades(f).obtener(concepto).declaracion());
    }

    @ParameterizedTest
    @MethodSource("correspondencias")
    void describirUnaSituacionNoLaConvierteEnNecesidad(String concepto, String necesidad, String capacidad) {
        n.setNecesidadesApoyo(capacidad.replace("Podemos", "Tiene capacidad para"));
        f.setNotaCondicionesSalud(capacidad);
        assertEquals(0, puntos("salud"));
        assertEquals(Declaracion.INSUFICIENTE, analizador.necesidades(n).obtener(concepto).declaracion());
    }

    @Test
    void noUsaNotasDelHogarParaAcreditarCondicionesMedicas() {
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
        f.setNotaEstructuraHogar("Podemos acompañar asma");
        f.setNotaRedApoyo("Podemos acompañar asma");
        assertEquals(0, puntos("salud"));
    }

    @Test
    void personalidadYDescripcionesGeneralesNoSonCapacidadesEspecializadas() {
        n.setNecesidadesApoyo("Necesita acompañamiento emocional específico");
        f.setNotaCondicionesSalud("Somos cariñosos, responsables y tenemos un buen hogar");
        assertEquals(0, puntos("salud"));
        n.setNecesidadesApoyo("Es introvertido. Presenta dificultades de adaptación");
        f.setNotaCondicionesSalud("Somos extrovertidos. Podemos acompañarlo durante su adaptación");
        assertEquals(0, puntos("salud"));
    }

    @Test
    void datosContextualesNoAlteranUnaCoincidenciaSocialValida() {
        n.setNecesidadesApoyo("Necesita rutinas estables");
        f.setNotaCondicionesSalud("Podemos mantener horarios constantes");
        int antes = puntos("salud");
        for (TipoFamilia tipo : TipoFamilia.values()) {
            f.setTipoFamilia(tipo);
            f.setContinuidadCultural("Otra cultura");
            f.setApoyoIdiomas("Otra lengua");
            n.setPersonalidadDetalle("Es introvertido");
            assertEquals(antes, puntos("salud"));
        }
    }

    static IntStream pesos() { return IntStream.rangeClosed(0, 100); }

    @ParameterizedTest
    @MethodSource("pesos")
    void necesidadesMultiplesRespetanPesosYRedondeo(int peso) {
        n.setNecesidadesApoyo("Necesita rutinas estables. Requiere acompañamiento emocional");
        f.setNotaCondicionesSalud("Podemos mantener horarios constantes. Podemos ofrecer acompañamiento emocional con apoyo");
        p.setEdad(100 - peso);
        p.setHermanos(0);
        p.setSalud(peso);
        p.setApoyo(0);
        p.setLogistica(0);
        assertEquals(Math.round(peso * .75), puntos("salud"));
        var r = motor.candidato(n, f, p);
        int suma = ((Map<?, ?>) r.get("f")).values().stream()
                .mapToInt(v -> ((Number) ((List<?>) v).get(0)).intValue()).sum();
        assertEquals(suma, r.get("puntaje"));
        assertTrue(puntos("salud") <= peso);
    }
}
