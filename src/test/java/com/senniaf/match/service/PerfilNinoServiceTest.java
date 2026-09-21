package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.model.enums.Alergia;
import com.senniaf.match.model.enums.AreaHabilidad;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.Interes;
import com.senniaf.match.model.enums.NivelEscolar;
import com.senniaf.match.model.enums.RasgoPersonalidad;
import com.senniaf.match.model.enums.RendimientoAcademico;
import com.senniaf.match.model.enums.RespuestaSiNoNoSabe;
import com.senniaf.match.model.enums.SiNo;
import com.senniaf.match.repository.PerfilNinoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilNinoServiceTest {

    private PerfilNinoService service;

    @BeforeEach
    void setUp() {
        service = new PerfilNinoService(new PerfilNinoRepository());
    }

    @Test
    void registraUnPerfilCompletoYDevuelveCodigoDeCaso() {
        PerfilNinoRequest request = perfilCompletoSinCondicionesMedicas();

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertEquals("NNA-001", response.getCodigoCaso());
        assertEquals("Perfil registrado correctamente", response.getMensaje());
        assertTrue(response.isPerfilCompleto());
        assertFalse(response.isAlertaMedica());
    }

    @Test
    void generaCodigosDeCasoSecuencialesParaCadaPerfil() {
        PerfilNinoRequest request = perfilCompletoSinCondicionesMedicas();

        PerfilNinoResponse primero = service.registrarPerfil(request, "trabajador-social-1");
        PerfilNinoResponse segundo = service.registrarPerfil(request, "trabajador-social-1");

        assertEquals("NNA-001", primero.getCodigoCaso());
        assertEquals("NNA-002", segundo.getCodigoCaso());
    }

    @Test
    void marcaComoIncompletoUnPerfilConCamposObligatoriosSinLlenar() {
        PerfilNinoRequest request = new PerfilNinoRequest();
        request.setEdad(8);
        request.setNivelEscolar(NivelEscolar.PRIMARIA);
        request.setNecesidadesApoyo("Terapia de lenguaje");
        // el resto de las listas (habilidades, gustos, personalidad,
        // alergias, condicionesSalud) y campos obligatorios queda sin llenar

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertFalse(response.isPerfilCompleto());
        assertTrue(response.getMensaje().contains("incompleto"));
    }

    @Test
    void marcaAlertaMedicaCuandoDeclaraUnaAlergia() {
        PerfilNinoRequest request = perfilCompletoSinCondicionesMedicas();
        request.setAlergias(List.of(Alergia.ALIMENTARIA));
        request.setAlergiasDetalle("Alergia al maní");

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertTrue(response.isAlertaMedica());
        assertTrue(response.getMensaje().contains("atención"));
    }

    @Test
    void marcaAlertaMedicaCuandoEsIntoleranteALaLactosa() {
        PerfilNinoRequest request = perfilCompletoSinCondicionesMedicas();
        request.setIntoleranciaLactosa(RespuestaSiNoNoSabe.SI);

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertTrue(response.isAlertaMedica());
    }

    @Test
    void noMarcaAlertaMedicaCuandoTodoEstaEnNingunaConocida() {
        PerfilNinoRequest request = perfilCompletoSinCondicionesMedicas();

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertFalse(response.isAlertaMedica());
    }

    private PerfilNinoRequest perfilCompletoSinCondicionesMedicas() {
        PerfilNinoRequest request = new PerfilNinoRequest();
        request.setEdad(10);
        request.setNivelEscolar(NivelEscolar.PRIMARIA);
        request.setGradoEscolar("5to grado");
        request.setRendimientoAcademico(RendimientoAcademico.BUENO);
        request.setHabilidades(List.of(AreaHabilidad.MATEMATICAS, AreaHabilidad.ARTE));
        request.setGustos(List.of(Interes.DEPORTES, Interes.VIDEOJUEGOS));
        request.setPersonalidad(List.of(RasgoPersonalidad.RESERVADO, RasgoPersonalidad.SOCIABLE));
        request.setAlergias(List.of(Alergia.NINGUNA_CONOCIDA));
        request.setIntoleranciaLactosa(RespuestaSiNoNoSabe.NO);
        request.setCondicionesSalud(List.of(CondicionSalud.NINGUNA_CONOCIDA));
        request.setNecesidadesApoyo("Apoyo educativo en lectura");
        request.setTieneHermanos(SiNo.SI);
        request.setCantidadHermanos(1);
        request.setHermanosDetalle("Tiene una hermana de 7 años");
        return request;
    }
}
