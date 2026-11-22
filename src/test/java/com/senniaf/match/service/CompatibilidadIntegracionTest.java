package com.senniaf.match.service;

import com.senniaf.match.controller.*;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(classes = CompatibilidadIntegracionTest.Config.class,
        properties = {"debug=false", "logging.level.root=WARN", "logging.level.org.springframework=WARN"})
@AutoConfigureMockMvc
class CompatibilidadIntegracionTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({ScoringService.class, ScoringController.class, GlobalExceptionHandler.class})
    static class Config {}

    @Autowired MockMvc mvc;
    @MockitoBean PerfilNinoRepository ninos;
    @MockitoBean PerfilFamiliaRepository familias;
    @MockitoBean DecisionComiteRepository decisiones;
    @MockitoBean PesosScoringRepository pesosRepo;
    @MockitoBean EventoCasoRepository auditoria;
    @MockitoBean VinculacionRepository vinculacionesRepo;
    @MockitoBean CombinacionExcluidaRepository excluidas;
    @MockitoBean InterrupcionConvivenciaRepository interrupciones;
    @MockitoBean VinculacionService vinculaciones;
    @MockitoBean PesosProgramadosService programados;
    @MockitoBean PerfilFamiliaService familiasService;
    @MockitoBean EvaluacionPsicologicaRepository evaluaciones;
    private PerfilNino n;
    private PerfilFamilia f;
    private PesosScoring pesos;

    @BeforeEach
    void preparar() {
        n = PerfilesCompatibilidad.nino();
        f = PerfilesCompatibilidad.familia();
        pesos = new PesosScoring();
        pesos.setApoyo(16);
        pesos.setLogistica(4);
        when(programados.vigentes()).thenReturn(pesos);
        when(ninos.findAll()).thenReturn(List.of(n));
        when(familias.findAll()).thenReturn(List.of(f));
        when(ninos.findByCodigoCaso(n.getCodigoCaso())).thenReturn(Optional.of(n));
    }

    @Test
    void expedienteAnalizadorServicioControladorYDesgloseJson() throws Exception {
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
        n.setNecesidadesApoyo("Necesita terapia de lenguaje");
        f.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA));
        f.setNotaCondicionesSalud("Podemos acompañar las terapias del lenguaje");
        mvc.perform(get("/api/scoring/casos").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].familias[0].f.salud[0]").value(35))
                .andExpect(jsonPath("$[0].familias[0].puntaje").value(75))
                .andExpect(jsonPath("$[0].familias[0].reglas").value("EVIDENCIA_TEXTO_7"))
                .andExpect(jsonPath("$[0].familias[0].maximoAplicable").value(75))
                .andExpect(jsonPath("$[0].familias[0].estadosFactores.hermanos").value("NO_APLICA"))
                .andExpect(jsonPath("$[0].familias[0].evidenciasCompatibilidad.necesidades.conceptos.TERAPIA_LENGUAJE.evidencias[0].campo").value("necesidadesApoyo"));
        f.setNotaCondicionesSalud("No podemos acompañar las terapias de lenguaje");
        mvc.perform(get("/api/scoring/casos/PRUEBA-N").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familias[0].f.salud[0]").value(18))
                .andExpect(jsonPath("$.familias[0].puntaje").value(58));
        verify(ninos, never()).save(any());
        verify(familias, never()).save(any());
        verify(decisiones, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void total65NoOcultaLaEdadFueraDelRango() throws Exception {
        n.setTieneHermanos(SiNo.SI);
        n.setCantidadHermanos(1);
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA));
        f.setEdadMinimaAceptada(12);
        f.setEdadMaximaAceptada(17);
        f.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA));
        f.setDisposicionHermanos(DisposicionHermanos.DISPUESTA_CON_DUDAS);
        f.setDisponibilidadViaje(DisponibilidadViaje.BAJA);
        mvc.perform(get("/api/scoring/casos/PRUEBA-N").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familias[0].puntaje").value(65))
                .andExpect(jsonPath("$.familias[0].f.edad[0]").value(0))
                .andExpect(jsonPath("$.familias[0].f.edad[1]").value("Edad fuera del rango declarado"));
    }

    @Test
    void maximoAplicableNoSeNormalizaA100() throws Exception {
        mvc.perform(get("/api/scoring/casos/PRUEBA-N").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familias[0].puntaje").value(40))
                .andExpect(jsonPath("$.familias[0].maximoAplicable").value(40))
                .andExpect(jsonPath("$.familias[0].pesoEvaluado").value(40))
                .andExpect(jsonPath("$.familias[0].estadosFactores.salud").value("NO_APLICA"));
    }

    @Test
    void acompanamientoPsicologicoLlegaAlDesgloseConSuProcedencia() throws Exception {
        n.setNecesidadesApoyo("Necesita acompañamiento psicológico semanal");
        f.setNotaCondicionesSalud("Podemos facilitar y acompañar sus sesiones psicológicas");
        mvc.perform(get("/api/scoring/casos/PRUEBA-N").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familias[0].f.salud[0]").value(35))
                .andExpect(jsonPath("$.familias[0].evidenciasCompatibilidad.necesidades.conceptos.ACOMPANAMIENTO_PSICOLOGICO.evidencias[0].campo").value("necesidadesApoyo"));
    }

    @Test
    void notasDelHogarYRedProducenCoberturaSocialRealSinDuplicarla() throws Exception {
        n.setNecesidadesApoyo("Necesita rutinas estables. Requiere acompañamiento emocional");
        n.setCondicionesSaludDetalle("Necesita una rutina diaria estable");
        f.setNotaEstructuraHogar("Podemos mantener horarios constantes");
        f.setNotaRedApoyo("Podemos ofrecer apoyo emocional con apoyo adicional");
        mvc.perform(get("/api/scoring/casos/PRUEBA-N").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familias[0].f.salud[0]").value(26))
                .andExpect(jsonPath("$.familias[0].f.apoyo[0]").value(16))
                .andExpect(jsonPath("$.familias[0].puntaje").value(66))
                .andExpect(jsonPath("$.familias[0].evidenciasCompatibilidad.capacidades.conceptos.RUTINA_ESTABLE.evidencias[0].campo").value("notaEstructuraHogar"))
                .andExpect(jsonPath("$.familias[0].evidenciasCompatibilidad.capacidades.conceptos.ACOMPANAMIENTO_EMOCIONAL.evidencias[0].campo").value("notaRedApoyo"));
        verify(ninos, never()).save(any());
        verify(familias, never()).save(any());
        verify(decisiones, never()).save(any());
    }

    @Test
    void recomendacionVigenteAlcanzaElDesgloseSinPublicarElInformeCompleto() throws Exception {
        var e = new EvaluacionPsicologica();
        ReflectionTestUtils.setField(e, "id", 100L);
        e.setExpediente(n);
        e.setNumeroVersion(1);
        e.setRecomendaciones("Requiere acompañamiento psicológico. TEXTO CLINICO PRIVADO");
        e.setEvaluacion("INFORME COMPLETO PRIVADO");
        when(evaluaciones.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso())).thenReturn(List.of(e));
        f.setNotaCondicionesSalud("Tenemos disponibilidad para asistir con el niño a terapia psicológica");
        mvc.perform(get("/api/scoring/casos/PRUEBA-N").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familias[0].f.salud[0]").value(35))
                .andExpect(jsonPath("$.familias[0].evidenciasCompatibilidad.necesidades.conceptos.ACOMPANAMIENTO_PSICOLOGICO.evidencias[0].campo")
                        .value("recomendacionesPsicologicas:100:v1"))
                .andExpect(content().string(not(containsString("TEXTO CLINICO PRIVADO"))))
                .andExpect(content().string(not(containsString("INFORME COMPLETO PRIVADO"))));
        verify(evaluaciones, times(1)).findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso());
        verify(evaluaciones, never()).save(any());
    }

    @Test
    void historialConservaPuntosPesosYFundamentoAnteriores() throws Exception {
        var decision = new DecisionComite();
        decision.setCodigoNino(n.getCodigoCaso());
        decision.setCodigoFamilia(f.getCodigoCaso());
        decision.setPuntaje(41);
        decision.setVersionPesos(1);
        decision.setContextoScoring("{\"reglas\":\"ANTERIOR\",\"factores\":{\"salud\":[7,\"Razón original\"]}}");
        when(decisiones.findByCodigoNinoOrderByFechaAsc(n.getCodigoCaso())).thenReturn(List.of(decision));
        mvc.perform(get("/api/scoring/casos/PRUEBA-N/historial").header("X-User-Role", "COMITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].puntaje").value(41))
                .andExpect(jsonPath("$[0].versionPesos").value(1))
                .andExpect(jsonPath("$[0].contextoScoring.reglas").value("ANTERIOR"))
                .andExpect(jsonPath("$[0].contextoScoring.factores.salud[0]").value(7));
        verify(decisiones, never()).save(any());
    }

    @Test
    void otrosRolesNoRecibenPuntuaciones() throws Exception {
        for (String rol : List.of("FAMILIA", "PSICOLOGO", "TRABAJADOR_SOCIAL")) {
            mvc.perform(get("/api/scoring/casos").header("X-User-Role", rol))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(ninos, familias);
    }
}
