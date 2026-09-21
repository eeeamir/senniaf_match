package com.senniaf.match.controller;

import com.senniaf.match.repository.PerfilNinoRepository;
import com.senniaf.match.service.PerfilNinoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilNinoControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PerfilNinoService service = new PerfilNinoService(new PerfilNinoRepository());
        PerfilNinoController controller = new PerfilNinoController(service);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void trabajadorSocialPuedeRegistrarUnPerfil() throws Exception {
        String cuerpo = """
                {
                  "edad": 10,
                  "nivelEscolar": "PRIMARIA",
                  "gradoEscolar": "5to grado",
                  "rendimientoAcademico": "BUENO",
                  "habilidades": ["MATEMATICAS", "ARTE"],
                  "gustos": ["DEPORTES", "VIDEOJUEGOS"],
                  "personalidad": ["RESERVADO", "SOCIABLE"],
                  "alergias": ["NINGUNA_CONOCIDA"],
                  "intoleranciaLactosa": "NO",
                  "condicionesSalud": ["NINGUNA_CONOCIDA"],
                  "necesidadesApoyo": "Apoyo educativo en lectura",
                  "tieneHermanos": "SI",
                  "cantidadHermanos": 1,
                  "hermanosDetalle": "Tiene una hermana de 7 años"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .header("X-User-Id", "ts-001")
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoCaso").value("NNA-001"))
                .andExpect(jsonPath("$.mensaje").value("Perfil registrado correctamente"))
                .andExpect(jsonPath("$.alertaMedica").value(false));
    }

    @Test
    void marcaAlertaMedicaCuandoElPerfilDeclaraCondicionesDeSalud() throws Exception {
        String cuerpo = """
                {
                  "edad": 9,
                  "nivelEscolar": "PRIMARIA",
                  "rendimientoAcademico": "REGULAR",
                  "habilidades": ["DEPORTES"],
                  "gustos": ["MUSICA"],
                  "personalidad": ["CARINOSO"],
                  "alergias": ["ALIMENTARIA"],
                  "alergiasDetalle": "Alergia al maní",
                  "intoleranciaLactosa": "SI",
                  "condicionesSalud": ["ASMA"],
                  "necesidadesApoyo": "Control periódico con neumólogo",
                  "tieneHermanos": "NO"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "PSICOLOGO")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertaMedica").value(true));
    }

    @Test
    void unaFamiliaNoPuedeRegistrarUnPerfil() throws Exception {
        String cuerpo = """
                {
                  "edad": 10,
                  "nivelEscolar": "PRIMARIA",
                  "rendimientoAcademico": "BUENO",
                  "habilidades": ["NINGUNA_IDENTIFICADA"],
                  "gustos": ["NINGUNO_IDENTIFICADO"],
                  "personalidad": ["RESERVADO"],
                  "alergias": ["NINGUNA_CONOCIDA"],
                  "intoleranciaLactosa": "NO",
                  "condicionesSalud": ["NINGUNA_CONOCIDA"],
                  "necesidadesApoyo": "Ninguna",
                  "tieneHermanos": "NO"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "FAMILIA")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaUnPerfilSinEdad() throws Exception {
        String cuerpo = """
                {
                  "nivelEscolar": "PRIMARIA",
                  "rendimientoAcademico": "BUENO",
                  "habilidades": ["NINGUNA_IDENTIFICADA"],
                  "gustos": ["NINGUNO_IDENTIFICADO"],
                  "personalidad": ["RESERVADO"],
                  "alergias": ["NINGUNA_CONOCIDA"],
                  "intoleranciaLactosa": "NO",
                  "condicionesSalud": ["NINGUNA_CONOCIDA"],
                  "necesidadesApoyo": "Ninguna",
                  "tieneHermanos": "NO"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnPerfilSinSeleccionarAlergias() throws Exception {
        String cuerpo = """
                {
                  "edad": 10,
                  "nivelEscolar": "PRIMARIA",
                  "rendimientoAcademico": "BUENO",
                  "habilidades": ["NINGUNA_IDENTIFICADA"],
                  "gustos": ["NINGUNO_IDENTIFICADO"],
                  "personalidad": ["RESERVADO"],
                  "intoleranciaLactosa": "NO",
                  "condicionesSalud": ["NINGUNA_CONOCIDA"],
                  "necesidadesApoyo": "Ninguna",
                  "tieneHermanos": "NO"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnValorNoValidoEnUnaListaDesplegable() throws Exception {
        String cuerpo = """
                {
                  "edad": 10,
                  "nivelEscolar": "UNIVERSIDAD",
                  "rendimientoAcademico": "BUENO",
                  "habilidades": ["NINGUNA_IDENTIFICADA"],
                  "gustos": ["NINGUNO_IDENTIFICADO"],
                  "personalidad": ["RESERVADO"],
                  "alergias": ["NINGUNA_CONOCIDA"],
                  "intoleranciaLactosa": "NO",
                  "condicionesSalud": ["NINGUNA_CONOCIDA"],
                  "necesidadesApoyo": "Ninguna",
                  "tieneHermanos": "NO"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .header("X-User-Id", "test-001")
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
