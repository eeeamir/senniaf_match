package com.senniaf.match.controller;
import com.senniaf.match.support.ServiciosDePrueba;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.repository.PerfilFamiliaRepository;
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
class PerfilFamiliaControllerTest {
 private MockMvc mvc;
 private static final String BODY="""
  {"tipoFamilia":"PAREJA","redApoyo":["FAMILIA_EXTENDIDA"],"provincia":"PANAMA",
   "disponibilidadViaje":"ALTA","edadMinimaAceptada":3,"edadMaximaAceptada":8,
   "disposicionHermanos":"DISPUESTA","condicionesPuedeAcompanarHoy":["ASMA"]}
 """;
 @BeforeEach void setUp(){
   PerfilFamiliaRepository repo=mock(PerfilFamiliaRepository.class);
   when(repo.save(any())).thenAnswer(i->i.getArgument(0));
   
   Usuario familia=new Usuario();familia.setRol("FAMILIA");familia.setIdentificador("8-123-456");
   Usuario trabajador=new Usuario();trabajador.setRol("TRABAJADOR_SOCIAL");trabajador.setIdentificador("TS-001");
   AutenticacionService auth=ServiciosDePrueba.autenticacion(java.util.Map.of("Bearer familia",familia,"Bearer ts",trabajador));
   mvc=MockMvcBuilders.standaloneSetup(new PerfilFamiliaController(new PerfilFamiliaService(repo),auth))
    .setControllerAdvice(new GlobalExceptionHandler()).build();
 }
 @Test void familiaRegistra() throws Exception{
   mvc.perform(post("/api/familias").header("Authorization","Bearer familia").contentType(MediaType.APPLICATION_JSON).content(BODY))
     .andExpect(status().isCreated()).andExpect(jsonPath("$.perfilCompleto").value(true));
 }
 @Test void funcionarioNoRegistraFamilia() throws Exception{
   mvc.perform(post("/api/familias").header("Authorization","Bearer ts").contentType(MediaType.APPLICATION_JSON).content(BODY))
      .andExpect(status().isForbidden());
 }
 @Test void formatoInvalido() throws Exception{
   mvc.perform(post("/api/familias").header("Authorization","Bearer familia").contentType(MediaType.APPLICATION_JSON)
     .content(BODY.replace("PANAMA","MARTE"))).andExpect(status().isBadRequest());
 }
}
