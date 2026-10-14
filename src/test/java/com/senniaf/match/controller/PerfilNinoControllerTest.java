package com.senniaf.match.controller;
import com.senniaf.match.support.ServiciosDePrueba;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.repository.PerfilNinoRepository;
import com.senniaf.match.security.*;
import com.senniaf.match.service.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PerfilNinoControllerTest {
 private MockMvc mvc; private PerfilNinoRepository repo;
 private static final String BODY="""
  {"edad":10,"nivelEscolar":"PRIMARIA","rendimientoAcademico":"BUENO",
   "habilidades":["MATEMATICAS"],"gustos":["DEPORTES"],"personalidad":["SOCIABLE"],
   "alergias":["NINGUNA_CONOCIDA"],"intoleranciaLactosa":"NO",
   "condicionesSalud":["NINGUNA_CONOCIDA"],"necesidadesApoyo":"Apoyo escolar", "tieneHermanos":"NO"}
 """;
 @BeforeEach void setUp(){
   repo=mock(PerfilNinoRepository.class);AuditoriaService audit=new ServiciosDePrueba.AuditoriaEnMemoria();
   when(repo.save(any())).thenAnswer(i->i.getArgument(0));
   
   Usuario trabajador=new Usuario();trabajador.setRol("TRABAJADOR_SOCIAL");trabajador.setIdentificador("TS-001");
   Usuario familia=new Usuario();familia.setRol("FAMILIA");familia.setIdentificador("8-123-456");
   AutenticacionService auth=ServiciosDePrueba.autenticacion(java.util.Map.of("Bearer ts",trabajador,"Bearer familia",familia));
   mvc=MockMvcBuilders.standaloneSetup(new PerfilNinoController(new PerfilNinoService(repo,audit),auth,audit))
     .setControllerAdvice(new GlobalExceptionHandler()).build();
 }
 @Test void trabajadorPuedeRegistrarPerfil() throws Exception{
   mvc.perform(post("/api/ninos").header("Authorization","Bearer ts").contentType(MediaType.APPLICATION_JSON).content(BODY))
     .andExpect(status().isCreated()).andExpect(jsonPath("$.perfilCompleto").value(true))
     .andExpect(jsonPath("$.codigoCaso").isNotEmpty());
 }
 @Test void familiaNoPuedeRegistrarPerfil() throws Exception{
   mvc.perform(post("/api/ninos").header("Authorization","Bearer familia").contentType(MediaType.APPLICATION_JSON).content(BODY))
     .andExpect(status().isForbidden());
 }
 @Test void sinTokenNoPuedeAcceder() throws Exception{
   mvc.perform(get("/api/ninos")).andExpect(status().isUnauthorized());
 }
 @Test void perfilMalFormadoNoSeRegistra() throws Exception{
   mvc.perform(post("/api/ninos").header("Authorization","Bearer ts").contentType(MediaType.APPLICATION_JSON)
      .content(BODY.replace("PRIMARIA","UNIVERSIDAD"))).andExpect(status().isBadRequest());
 }
}
