package com.senniaf.match.iso25010.seguridad;

import com.senniaf.match.controller.*;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.service.PerfilFamiliaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FamiliaExpedienteIsoTest {
    private MockMvc mvc;
    @BeforeEach void setUp(){
        PerfilFamiliaService service=mock(PerfilFamiliaService.class);
        var expediente=new PerfilFamiliaResponse("Perfil consultado","SINT-FAM-01",true,false,0,false);
        var detalle=new com.senniaf.match.model.PerfilFamilia(); detalle.setCodigoCaso("SINT-FAM-01"); detalle.setPerfilCompleto(true);
        doReturn(Optional.of(expediente)).when(service).consultarPorCedula(anyString());
        doReturn(Optional.of(detalle)).when(service).consultarDetallePropio(anyString());
        mvc=MockMvcBuilders.standaloneSetup(new PerfilFamiliaController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void SEG_FAM_01_miPerfilPropioDevuelve200SinDatosInfantilesNiPuntajes() throws Exception {
        mvc.perform(get("/api/familias/mi-perfil").header("X-User-Role","FAMILIA").header("X-User-Id","SINT-FAM-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigoCaso").value("SINT-FAM-01"))
                .andExpect(jsonPath("$.puntaje").doesNotExist()).andExpect(jsonPath("$.nino").doesNotExist());
    }
    @Test void SEG_FAM_02_detallePropioDevuelve200SinDatosInfantilesNiPuntajes() throws Exception {
        mvc.perform(get("/api/familias/mi-perfil/detalle").header("X-User-Role","FAMILIA").header("X-User-Id","SINT-FAM-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigoCaso").value("SINT-FAM-01"))
                .andExpect(jsonPath("$.puntaje").doesNotExist()).andExpect(jsonPath("$.nino").doesNotExist());
    }
}
