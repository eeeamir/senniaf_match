package com.senniaf.match.controller;

import com.senniaf.match.repository.PerfilFamiliaRepository;
import com.senniaf.match.service.PerfilFamiliaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilFamiliaControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PerfilFamiliaService service = new PerfilFamiliaService(new PerfilFamiliaRepository());
        PerfilFamiliaController controller = new PerfilFamiliaController(service);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String CUERPO_VALIDO = """
            {
              "tipoFamilia": "PAREJA",
              "notaEstructuraHogar": "Vivimos juntos hace 8 años",
              "redApoyo": ["PAREJA", "FAMILIA_EXTENDIDA"],
              "notaRedApoyo": "Mis padres viven cerca",
              "provincia": "PANAMA",
              "disponibilidadViaje": "ALTA",
              "notaDistancia": "Podemos viajar seguido",
              "edadMinimaAceptada": 3,
              "edadMaximaAceptada": 8,
              "notaRangoEdad": "Nos sentimos preparados para edad escolar",
              "disposicionHermanos": "DISPUESTA_CON_DUDAS",
              "notaHermanos": "Nos preocupa el espacio en la casa",
              "condicionesPuedeAcompanarHoy": ["ASMA"],
              "condicionesConApoyo": ["TDAH"],
              "notaCondicionesSalud": "Nos gustaría capacitación sobre TDAH",
              "continuidadCultural": "Tenemos raíces indígenas"
            }
            """;

    @Test
    void unaFamiliaPuedeRegistrarSuPropioPerfil() throws Exception {
        mockMvc.perform(post("/api/familias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "FAMILIA")
                        .header("X-User-Id", "familia-001")
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoCaso").value("FAM-001"))
                .andExpect(jsonPath("$.mensaje").value("Perfil registrado correctamente"))
                .andExpect(jsonPath("$.perfilCompleto").value(true));
    }

    @Test
    void unTrabajadorSocialNoPuedeRegistrarUnPerfilDeFamiliaPorEsteEndpoint() throws Exception {
        mockMvc.perform(post("/api/familias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .header("X-User-Id", "test-001")
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void unPsicologoNoPuedeRegistrarUnPerfilDeFamilia() throws Exception {
        mockMvc.perform(post("/api/familias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "PSICOLOGO")
                        .header("X-User-Id", "test-001")
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaUnPerfilSinTipoDeFamilia() throws Exception {
        String cuerpo = """
                {
                  "redApoyo": ["PAREJA"],
                  "provincia": "PANAMA",
                  "disponibilidadViaje": "ALTA",
                  "edadMinimaAceptada": 3,
                  "edadMaximaAceptada": 8,
                  "disposicionHermanos": "DISPUESTA"
                }
                """;

        mockMvc.perform(post("/api/familias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "FAMILIA")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnValorNoValidoEnUnaListaDesplegable() throws Exception {
        String cuerpo = """
                {
                  "tipoFamilia": "PAREJA",
                  "redApoyo": ["PAREJA"],
                  "provincia": "MARTE",
                  "disponibilidadViaje": "ALTA",
                  "edadMinimaAceptada": 3,
                  "edadMaximaAceptada": 8,
                  "disposicionHermanos": "DISPUESTA"
                }
                """;

        mockMvc.perform(post("/api/familias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "FAMILIA")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptaUnPerfilSinDeclararNingunaCondicionDeSalud() throws Exception {
        String cuerpo = """
                {
                  "tipoFamilia": "MONOPARENTAL",
                  "redApoyo": ["NINGUNA_POR_AHORA"],
                  "provincia": "CHIRIQUI",
                  "disponibilidadViaje": "BAJA",
                  "edadMinimaAceptada": 6,
                  "edadMaximaAceptada": 12,
                  "disposicionHermanos": "PREFIERE_UN_SOLO_NINO"
                }
                """;

        mockMvc.perform(post("/api/familias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "FAMILIA")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfilCompleto").value(true));
    }
}
