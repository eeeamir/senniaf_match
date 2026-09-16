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
                  "escolaridad": "5to grado",
                  "rendimientoAcademico": "Bueno",
                  "habilidades": "Matemáticas y dibujo",
                  "gustos": "Fútbol, videojuegos y dibujar",
                  "personalidad": "Reservado al principio, pero sociable cuando adquiere confianza",
                  "necesidades": "Apoyo educativo en lectura",
                  "hermanos": "Tiene una hermana de 7 años"
                }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .header("X-User-Id", "ts-001")
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoCaso").value("NNA-001"))
                .andExpect(jsonPath("$.mensaje").value("Perfil registrado correctamente"));
    }

    @Test
    void unaFamiliaNoPuedeRegistrarUnPerfil() throws Exception {
        String cuerpo = """
                { "edad": 10, "escolaridad": "5to grado", "necesidades": "Ninguna" }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "FAMILIA")
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaUnPerfilSinEdad() throws Exception {
        String cuerpo = """
                { "escolaridad": "5to grado", "necesidades": "Ninguna" }
                """;

        mockMvc.perform(post("/api/ninos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "TRABAJADOR_SOCIAL")
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
