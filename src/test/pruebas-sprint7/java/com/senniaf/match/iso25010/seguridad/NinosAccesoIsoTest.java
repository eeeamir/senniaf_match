package com.senniaf.match.iso25010.seguridad;

import com.senniaf.match.controller.PerfilNinoController;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.HistorialCasoService;
import com.senniaf.match.service.PerfilNinoService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Matriz de autorización del controlador /api/ninos con identidad sintética. */
class NinosAccesoIsoTest {
    static Stream<Arguments> matriz() {
        List<String> roles=List.of("TRABAJADOR_SOCIAL","PSICOLOGO","COMITE","FAMILIA");
        List<String> endpoints=List.of("GET_LISTAR","POST_REGISTRAR","GET_DETALLE","GET_AUDITORIA","PUT_EDITAR","DELETE_BAJA",
                "GET_EVALUACIONES","POST_EVALUACION","PUT_EVALUACION","GET_PENDIENTES","GET_SOLICITUDES","POST_SOLICITUD",
                "POST_RESOLUCION");
        List<Arguments> out=new ArrayList<>(); int n=1;
        for(String ep:endpoints) for(String rol:roles) out.add(Arguments.of("SEG-NIN-"+String.format("%02d",n++),ep,rol));
        return out.stream();
    }

    @ParameterizedTest(name="{0} {1} rol={2}")
    @MethodSource("matriz")
    void SEG_NIN_matrizEndpointRol(String id,String endpoint,String rol) {
        PerfilNinoService service=mock(PerfilNinoService.class);
        when(service.listarPerfiles(anyString(),anyString(),anyBoolean())).thenReturn(List.of());
        when(service.numeroSolicitudesPendientes(any())).thenReturn(0L);
        PerfilNinoController controller=new PerfilNinoController(service,mock(HistorialCasoService.class));
        boolean permitido=permitido(endpoint,rol);
        if(permitido) assertDoesNotThrow(()->invocar(controller,endpoint,rol));
        else assertThrows(RolNoAutorizadoException.class,()->invocar(controller,endpoint,rol));
    }

    private static boolean permitido(String ep,String rol) {
        return switch(ep) {
            case "GET_LISTAR","GET_DETALLE","GET_AUDITORIA","GET_EVALUACIONES","GET_SOLICITUDES" ->
                    Set.of("TRABAJADOR_SOCIAL","PSICOLOGO","COMITE").contains(rol);
            case "POST_REGISTRAR","PUT_EDITAR","POST_SOLICITUD" -> "TRABAJADOR_SOCIAL".equals(rol);
            case "POST_EVALUACION","PUT_EVALUACION" -> "PSICOLOGO".equals(rol);
            case "GET_PENDIENTES","POST_RESOLUCION" -> "COMITE".equals(rol);
            case "DELETE_BAJA" -> false;
            default -> false;
        };
    }

    private static void invocar(PerfilNinoController c,String ep,String rol) {
        switch(ep) {
            case "GET_LISTAR" -> c.listarPerfiles(rol,"SINT-USER",false);
            case "POST_REGISTRAR" -> c.registrarPerfil(null,rol,"SINT-USER");
            case "GET_DETALLE" -> c.detalle("SINT-NINO",rol,"SINT-USER");
            case "GET_AUDITORIA" -> c.auditoria("SINT-NINO",rol,"SINT-USER");
            case "PUT_EDITAR" -> c.editar("SINT-NINO",null,rol,"SINT-USER");
            case "DELETE_BAJA" -> c.baja("SINT-NINO",rol,"SINT-USER");
            case "GET_EVALUACIONES" -> c.evaluaciones("SINT-NINO",rol,"SINT-USER");
            case "POST_EVALUACION" -> c.evaluar("SINT-NINO",null,rol,"SINT-USER");
            case "PUT_EVALUACION" -> c.corregirEvaluacion("SINT-NINO",1L,null,rol,"SINT-USER");
            case "GET_PENDIENTES" -> c.pendientes(rol);
            case "GET_SOLICITUDES" -> c.solicitudes("SINT-NINO",rol,"SINT-USER");
            case "POST_SOLICITUD" -> c.solicitar("SINT-NINO",null,rol,"SINT-USER");
            case "POST_RESOLUCION" -> c.resolver(1L,null,rol,"SINT-USER");
            default -> throw new IllegalArgumentException(ep);
        }
    }
}
