package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompatibilidadMotorTest {
    private final ScoringService motor = new ScoringService(mock(PerfilNinoRepository.class),
            mock(PerfilFamiliaRepository.class), mock(DecisionComiteRepository.class), mock(PesosScoringRepository.class));
    private PerfilNino n;
    private PerfilFamilia f;
    private PesosScoring pesos;

    @BeforeEach
    void preparar() {
        n = PerfilesCompatibilidad.nino();
        f = PerfilesCompatibilidad.familia();
        pesos = new PesosScoring();
    }

    static IntStream pesosAdmitidos() { return IntStream.rangeClosed(0, 100); }

    private int puntos(Map<String, Object> resultado, String criterio) {
        return ((Number) ((List<?>) ((Map<?, ?>) resultado.get("f")).get(criterio)).get(0)).intValue();
    }

    private String estado(Map<String, Object> resultado, String criterio) {
        return (String) ((Map<?, ?>) resultado.get("estadosFactores")).get(criterio);
    }

    private Map<String, Object> resultado() { return motor.candidato(n, f, pesos); }

    private void asma() {
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
        f.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA));
    }

    @ParameterizedTest
    @CsvSource({"12,12,17,20", "17,12,17,20", "10,12,17,0", "10,11,9,0"})
    void limitesDeEdad(int edad, int min, int max, int esperado) {
        n.setEdad(edad);
        f.setEdadMinimaAceptada(min);
        f.setEdadMaximaAceptada(max);
        assertEquals(esperado, puntos(resultado(), "edad"));
        if (min > max) assertEquals("DESCONOCIDO", estado(resultado(), "edad"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 11})
    void mesesPertenecenAlTramoCero(int meses) {
        n.setEdad(meses);
        n.setUnidadEdad(UnidadEdad.MESES);
        f.setEdadMinimaAceptada(0);
        f.setEdadMaximaAceptada(0);
        assertEquals(pesos.getEdad(), puntos(resultado(), "edad"));
        f.setEdadMinimaAceptada(1);
        f.setEdadMaximaAceptada(17);
        assertEquals(0, puntos(resultado(), "edad"));
    }

    @Test
    void edadNulaYRangoNuloSonDesconocidos() {
        n.setEdad(null);
        assertEquals("DESCONOCIDO", estado(resultado(), "edad"));
        n.setEdad(10);
        f.setEdadMaximaAceptada(null);
        assertEquals("DESCONOCIDO", estado(resultado(), "edad"));
    }

    @ParameterizedTest
    @EnumSource(DisposicionHermanos.class)
    void hermanosPorDisposicionSinPenalizarCantidad(DisposicionHermanos disposicion) {
        n.setTieneHermanos(SiNo.SI);
        n.setCantidadHermanos(1);
        f.setDisposicionHermanos(disposicion);
        int esperado = switch (disposicion) {
            case DISPUESTA -> pesos.getHermanos();
            case DISPUESTA_CON_DUDAS -> Math.round(pesos.getHermanos() * .5f);
            default -> 0;
        };
        assertEquals(esperado, puntos(resultado(), "hermanos"));
        n.setCantidadHermanos(5);
        assertEquals(esperado, puntos(resultado(), "hermanos"));
    }

    @Test
    void hermanosNoAplicablesIncompletosYContradictorios() {
        assertEquals("NO_APLICA", estado(resultado(), "hermanos"));
        n.setTieneHermanos(SiNo.SI);
        n.setCantidadHermanos(null);
        assertEquals("DESCONOCIDO", estado(resultado(), "hermanos"));
        n.setCantidadHermanos(1);
        f.setNotaHermanos("No podemos acompañar hermanos");
        assertEquals(0, puntos(resultado(), "hermanos"));
        assertEquals("DESCONOCIDO", estado(resultado(), "hermanos"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Podemos acompañar las terapias de lenguaje|35",
        "Podemos acompañar las terapias de lenguaje con apoyo|26",
        "No podemos acompañar las terapias de lenguaje|18",
        "Quizás podríamos acompañar las terapias de lenguaje|18",
        "Mi hermano recibe terapias de lenguaje|18"
    })
    void necesidadesEstructuradasYTextualesCompartenElPeso(String nota, int esperado) {
        asma();
        n.setNecesidadesApoyo("Necesita terapia de lenguaje");
        f.setNotaCondicionesSalud(nota);
        assertEquals(esperado, puntos(resultado(), "salud"));
    }

    @Test
    void ejemploCompletoDelUsuarioReconoceDosCorrespondencias() {
        n.setCondicionesSalud(List.of(CondicionSalud.OTRA));
        n.setCondicionesSaludDetalle("Presenta asma y requiere acompañamiento en terapia de lenguaje");
        f.setNotaCondicionesSalud("Podemos atender a niños con asma y acompañarlos a sus terapias de lenguaje");
        assertEquals(pesos.getSalud(), puntos(resultado(), "salud"));
    }

    @Test
    void duplicadosYOrdenNoModificanPuntosNiDenominador() {
        asma();
        var antes = resultado();
        n.setCondicionesSaludDetalle("Presenta asma. ASMA. Presenta asma");
        f.setNotaCondicionesSalud("Puedo acompañar asma. Puedo acompañar ASMA");
        assertEquals(puntos(antes, "salud"), puntos(resultado(), "salud"));
        n.setNecesidadesApoyo("Necesita terapia de lenguaje. ASMA");
        f.setNotaCondicionesSalud("Podemos acompañar terapia del lenguaje. Puedo acompañar asma");
        int puntos = puntos(resultado(), "salud");
        f.setNotaCondicionesSalud("Puedo acompañar asma. Podemos acompañar terapia de lenguaje");
        assertEquals(puntos, puntos(resultado(), "salud"));
    }

    @Test
    void otraNoEsUnaEquivalencia() {
        n.setCondicionesSalud(List.of(CondicionSalud.OTRA));
        f.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.OTRA));
        assertEquals(0, puntos(resultado(), "salud"));
        assertEquals("DESCONOCIDO", estado(resultado(), "salud"));
        n.setCondicionesSaludDetalle("Necesita terapia de lenguaje");
        f.setNotaCondicionesSalud("Podemos acompañar terapias del lenguaje");
        assertEquals(pesos.getSalud(), puntos(resultado(), "salud"));
    }

    @Test
    void necesidadesSinEquivalenciaNoDesaparecenDelCalculo() {
        asma();
        n.setNecesidadesApoyo("Necesita tratamiento especializado");
        assertEquals(18, puntos(resultado(), "salud"));
        assertEquals("DESCONOCIDO", estado(resultado(), "salud"));
    }

    @ParameterizedTest
    @EnumSource(value = CondicionSalud.class, names = {"NINGUNA_CONOCIDA", "OTRA", "INFORMACION_NO_DISPONIBLE"}, mode = EnumSource.Mode.EXCLUDE)
    void todasLasCondicionesEspecificasSeComparanSinPenalizacionPorDiagnostico(CondicionSalud condicion) {
        n.setCondicionesSalud(List.of(condicion));
        f.setCondicionesPuedeAcompanarHoy(List.of(condicion));
        assertEquals(pesos.getSalud(), puntos(resultado(), "salud"));
        f.setCondicionesPuedeAcompanarHoy(List.of());
        f.setCondicionesConApoyo(List.of(condicion));
        assertEquals(Math.round(pesos.getSalud() * .5f), puntos(resultado(), "salud"));
    }

    @Test
    void alergiasYLactosaSeMantienenComoNecesidadesDistintas() {
        n.setAlergias(List.of(Alergia.ALIMENTARIA));
        n.setIntoleranciaLactosa(RespuestaSiNoNoSabe.SI);
        f.setNotaCondicionesSalud("Podemos acompañar alergias alimentarias. Podemos atender intolerancia a la lactosa con apoyo");
        assertEquals(26, puntos(resultado(), "salud"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ALIMENTARIA|alergias alimentarias", "MEDICAMENTOS|alergia a medicamentos",
        "AMBIENTAL_POLEN|alergia al polen", "PICADURA_INSECTOS|alergia a picaduras de insectos"
    })
    void alergiasEstructuradasTienenCapacidadTextualCorrespondiente(Alergia alergia, String texto) {
        n.setAlergias(List.of(alergia));
        f.setNotaCondicionesSalud("Podemos acompañar " + texto);
        assertEquals(pesos.getSalud(), puntos(resultado(), "salud"));
    }

    @Test
    void capacidadParaOtraNecesidadNoReduceLaCoberturaExistente() {
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
        f.setNotaCondicionesSalud("Podemos acompañar asma");
        int antes = puntos(resultado(), "salud");
        f.setNotaCondicionesSalud("Podemos acompañar asma y atender diabetes con apoyo");
        assertEquals(antes, puntos(resultado(), "salud"));
    }

    @Test
    void contradiccionesNuncaEligenLaCoberturaMasAlta() {
        asma();
        f.setNotaCondicionesSalud("No puedo acompañar asma");
        assertEquals(0, puntos(resultado(), "salud"));
        assertEquals("DESCONOCIDO", estado(resultado(), "salud"));
        f.setNotaCondicionesSalud(null);
        f.setCondicionesConApoyo(List.of(CondicionSalud.ASMA));
        assertEquals(0, puntos(resultado(), "salud"));
    }

    @Test
    void acompanamientoPsicologicoExplicitoSeComparaDentroDeSalud() {
        n.setNecesidadesApoyo("Necesita acompañamiento psicológico semanal");
        f.setNotaCondicionesSalud("Podemos facilitar y acompañar sus sesiones psicológicas");
        assertEquals(pesos.getSalud(), puntos(resultado(), "salud"));
        f.setNotaCondicionesSalud("Podemos acompañar las sesiones psicológicas con apoyo adicional");
        assertEquals(Math.round(pesos.getSalud() * .5f), puntos(resultado(), "salud"));
    }

    @Test
    void intencionGeneralOPersonalidadNoAcreditanCapacidadPsicologica() {
        n.setNecesidadesApoyo("Necesita acompañamiento psicológico");
        f.setNotaCondicionesSalud("Somos una familia amorosa. Podríamos buscar ayuda profesional si fuese necesaria");
        assertEquals(0, puntos(resultado(), "salud"));
        n.setNecesidadesApoyo("Es introvertido");
        n.setPersonalidadDetalle("Es una persona reservada");
        f.setNotaCondicionesSalud("Somos extrovertidos");
        assertEquals("NO_APLICA", estado(resultado(), "salud"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"Necesita una rutina estable|35", "Requiere apoyo educativo individualizado|35"})
    void necesidadesSocialesSoloAcreditanConceptosConRegla(String necesidad, int esperado) {
        n.setNecesidadesApoyo(necesidad);
        f.setNotaCondicionesSalud("Podemos mantener horarios y rutinas constantes. Podemos ofrecer apoyo educativo individualizado");
        assertEquals(esperado, puntos(resultado(), "salud"));
    }

    @Test
    void redGeneralNoPremiaElNumeroDeFuentes() {
        int antes = puntos(resultado(), "apoyo");
        f.setRedApoyo(List.of(FuenteApoyo.PAREJA, FuenteApoyo.FAMILIA_EXTENDIDA, FuenteApoyo.AMISTADES_O_COMUNIDAD));
        assertEquals(antes, puntos(resultado(), "apoyo"));
        f.setRedApoyo(List.of(FuenteApoyo.NINGUNA_POR_AHORA));
        assertEquals(0, puntos(resultado(), "apoyo"));
        assertEquals("EVALUADO", estado(resultado(), "apoyo"));
        f.setNotaRedApoyo("Contamos con una red de apoyo");
        assertEquals("DESCONOCIDO", estado(resultado(), "apoyo"));
    }

    @Test
    void limitacionDeUnaPersonaNoAnulaOtraFuenteDeclarada() {
        f.setRedApoyo(List.of(FuenteApoyo.PAREJA));
        f.setNotaRedApoyo("Mi hermana no puede ayudarnos");
        assertEquals(pesos.getApoyo(), puntos(resultado(), "apoyo"));
        f.setNotaRedApoyo("Mi hermana no puede ayudarnos, pero mi pareja sí");
        assertEquals(pesos.getApoyo(), puntos(resultado(), "apoyo"));
        f.setRedApoyo(List.of(FuenteApoyo.FAMILIA_EXTENDIDA));
        f.setNotaRedApoyo("Mi hermana no puede ayudarnos");
        assertEquals("DESCONOCIDO", estado(resultado(), "apoyo"));
        assertEquals(0, puntos(resultado(), "apoyo"));
    }

    @ParameterizedTest
    @EnumSource(DisponibilidadViaje.class)
    void logisticaConservaProporciones(DisponibilidadViaje viaje) {
        f.setDisponibilidadViaje(viaje);
        int esperado = switch (viaje) {
            case ALTA -> pesos.getLogistica();
            case MEDIA -> Math.round(pesos.getLogistica() * .6f);
            case BAJA -> Math.round(pesos.getLogistica() * .3f);
        };
        assertEquals(esperado, puntos(resultado(), "logistica"));
        f.setNotaDistancia("No podemos viajar");
        assertEquals(0, puntos(resultado(), "logistica"));
        assertEquals("DESCONOCIDO", estado(resultado(), "logistica"));
        f.setNotaDistancia("No podemos viajar, pero podemos recibir visitas");
        assertEquals(esperado, puntos(resultado(), "logistica"));
    }

    @Test
    void informacionAusenteNoEsRechazoNiAfirmacion() {
        n.setCondicionesSalud(null);
        f.setRedApoyo(null);
        f.setDisponibilidadViaje(null);
        var r = resultado();
        for (String criterio : List.of("salud", "apoyo", "logistica")) {
            assertEquals(0, puntos(r, criterio));
            assertEquals("DESCONOCIDO", estado(r, criterio));
        }
    }

    @ParameterizedTest
    @MethodSource("pesosAdmitidos")
    void pesosExtremosRedondeoMonotoniaYSuma(int peso) {
        asma();
        n.setNecesidadesApoyo("Necesita terapia de lenguaje");
        pesos.setEdad(100 - peso);
        pesos.setHermanos(0);
        pesos.setSalud(peso);
        pesos.setApoyo(0);
        pesos.setLogistica(0);
        int anterior = puntos(resultado(), "salud");
        assertEquals(Math.round(peso * .5), anterior);
        f.setNotaCondicionesSalud("Podemos acompañar terapia de lenguaje con apoyo");
        assertEquals(Math.round(peso * .75), puntos(resultado(), "salud"));
        f.setNotaCondicionesSalud("Podemos acompañar terapia de lenguaje");
        var r = resultado();
        assertEquals(peso, puntos(r, "salud"));
        assertTrue(puntos(r, "salud") >= anterior);
        verificarInvariantes(r, pesos);
    }

    @Test
    void combinacionesValidasDeLosCincoPesos() {
        var azar = new Random(9126);
        asma();
        n.setTieneHermanos(SiNo.SI);
        n.setCantidadHermanos(2);
        for (int i = 0; i < 500; i++) {
            int[] cortes = IntStream.concat(IntStream.of(0, 100), azar.ints(4, 0, 101)).sorted().toArray();
            pesos.setEdad(cortes[1] - cortes[0]);
            pesos.setHermanos(cortes[2] - cortes[1]);
            pesos.setSalud(cortes[3] - cortes[2]);
            pesos.setApoyo(cortes[4] - cortes[3]);
            pesos.setLogistica(cortes[5] - cortes[4]);
            var r = resultado();
            verificarInvariantes(r, pesos);
            assertEquals(100, r.get("puntaje"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"edad", "hermanos", "salud", "apoyo", "logistica"})
    void cadaCriterioPuedeTenerTodoElPeso(String criterio) {
        asma();
        n.setTieneHermanos(SiNo.SI);
        n.setCantidadHermanos(1);
        pesos.setEdad(criterio.equals("edad") ? 100 : 0);
        pesos.setHermanos(criterio.equals("hermanos") ? 100 : 0);
        pesos.setSalud(criterio.equals("salud") ? 100 : 0);
        pesos.setApoyo(criterio.equals("apoyo") ? 100 : 0);
        pesos.setLogistica(criterio.equals("logistica") ? 100 : 0);
        var r = resultado();
        assertEquals(100, puntos(r, criterio));
        assertEquals(100, r.get("puntaje"));
        verificarInvariantes(r, pesos);
    }

    private void verificarInvariantes(Map<String, Object> r, PesosScoring p) {
        var valores = Map.of("edad", p.getEdad(), "hermanos", p.getHermanos(), "salud", p.getSalud(), "apoyo", p.getApoyo(), "logistica", p.getLogistica());
        int suma = 0, noAplicable = 0, evaluado = 0;
        for (var e : valores.entrySet()) {
            int puntos = puntos(r, e.getKey());
            assertTrue(puntos >= 0 && puntos <= e.getValue());
            suma += puntos;
            if (estado(r, e.getKey()).equals("NO_APLICA")) noAplicable += e.getValue();
            if (estado(r, e.getKey()).equals("EVALUADO")) evaluado += e.getValue();
        }
        assertEquals(suma, r.get("puntaje"));
        assertEquals(100 - noAplicable, r.get("maximoAplicable"));
        assertEquals(evaluado, r.get("pesoEvaluado"));
    }

    @ParameterizedTest
    @EnumSource(TipoFamilia.class)
    void tipoDeFamiliaNoModificaPuntos(TipoFamilia tipo) {
        var antes = resultado();
        f.setTipoFamilia(tipo);
        assertEquals(antes, resultado());
    }

    @ParameterizedTest
    @EnumSource(ProvinciaPanama.class)
    void provinciaNoModificaLosPuntos(ProvinciaPanama provincia) {
        var antes = resultado();
        f.setProvincia(provincia);
        assertEquals(antes.get("f"), resultado().get("f"));
        assertEquals(antes.get("puntaje"), resultado().get("puntaje"));
    }

    @Test
    void culturaHogarPersonalidadYDatosAdministrativosNoModificanFactores() {
        var antes = resultado();
        n.setPersonalidadDetalle("Introvertido, reservado");
        n.setNecesidadesApoyo("Es introvertido");
        n.setNumeroExpedienteInstitucional("OTRO-IDENTIFICADOR");
        n.setIdiomaComunicacion("Otra lengua");
        n.setCostumbresTradiciones("Otras tradiciones");
        n.setVinculosCulturales("Otros vínculos");
        n.setOpinionCultural("Otra preferencia cultural");
        n.setRecomendacionesCulturales("Otros apoyos culturales");
        f.setNotaEstructuraHogar("Vivimos con familiares");
        f.setContinuidadCultural("Otra cultura");
        f.setApoyoIdiomas("Otro idioma");
        f.setRespetoTradiciones("Otras tradiciones");
        f.setRegistradoPor("OTRO-USUARIO");
        assertEquals(antes.get("f"), resultado().get("f"));
    }

    @ParameterizedTest
    @MethodSource("pesosAdmitidos")
    void redondeoDeHermanosYLogisticaConPesosPequenosYExtremos(int peso) {
        n.setTieneHermanos(SiNo.SI);
        n.setCantidadHermanos(2);
        f.setDisposicionHermanos(DisposicionHermanos.DISPUESTA_CON_DUDAS);
        pesos.setEdad(100 - peso);
        pesos.setHermanos(peso);
        pesos.setSalud(0);
        pesos.setApoyo(0);
        pesos.setLogistica(0);
        assertEquals(Math.round(peso * .5f), puntos(resultado(), "hermanos"));
        pesos.setHermanos(0);
        pesos.setLogistica(peso);
        f.setDisponibilidadViaje(DisponibilidadViaje.MEDIA);
        assertEquals(Math.round(peso * .6f), puntos(resultado(), "logistica"));
        f.setDisponibilidadViaje(DisponibilidadViaje.BAJA);
        var r = resultado();
        assertEquals(Math.round(peso * .3f), puntos(r, "logistica"));
        verificarInvariantes(r, pesos);
    }

    @Test
    void longitudDeTextoIrrelevanteNoIncrementaLosPuntos() {
        asma();
        var antes = resultado();
        f.setNotaCondicionesSalud("Somos una familia amorosa. ".repeat(20));
        assertEquals(antes.get("f"), resultado().get("f"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"idioma", "tradiciones", "vinculos", "opinion", "recomendaciones",
            "hogar", "personalidad", "expediente", "responsable"})
    void cambiarUnaCaracteristicaContextualNoAlteraElCalculo(String campo) {
        var antes = resultado();
        switch (campo) {
            case "idioma" -> n.setIdiomaComunicacion("Otra lengua");
            case "tradiciones" -> n.setCostumbresTradiciones("Otras tradiciones");
            case "vinculos" -> n.setVinculosCulturales("Otros vínculos");
            case "opinion" -> n.setOpinionCultural("Otra preferencia");
            case "recomendaciones" -> n.setRecomendacionesCulturales("Otra recomendación");
            case "hogar" -> f.setNotaEstructuraHogar("Vivimos con familiares");
            case "personalidad" -> n.setPersonalidadDetalle("Es introvertido");
            case "expediente" -> n.setNumeroExpedienteInstitucional("OTRO-EXPEDIENTE");
            case "responsable" -> f.setRegistradoPor("OTRO-RESPONSABLE");
            default -> fail("Campo de invariancia no previsto");
        }
        var despues = resultado();
        for (String clave : List.of("f", "puntaje", "maximoAplicable", "pesoEvaluado", "estadosFactores")) {
            assertEquals(antes.get(clave), despues.get(clave), campo + ": " + clave);
        }
    }
}
