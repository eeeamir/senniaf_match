package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.repository.PerfilNinoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        PerfilNinoRequest request = new PerfilNinoRequest();
        request.setEdad(10);
        request.setEscolaridad("5to grado");
        request.setRendimientoAcademico("Bueno");
        request.setHabilidades("Matemáticas y dibujo");
        request.setGustos("Fútbol, videojuegos y dibujar");
        request.setPersonalidad("Reservado al principio, pero sociable cuando adquiere confianza");
        request.setNecesidades("Apoyo educativo en lectura");
        request.setHermanos("Tiene una hermana de 7 años");

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertEquals("NNA-001", response.getCodigoCaso());
        assertEquals("Perfil registrado correctamente", response.getMensaje());
        assertTrue(response.isPerfilCompleto());
    }

    @Test
    void generaCodigosDeCasoSecuencialesParaCadaPerfil() {
        PerfilNinoRequest request = perfilMinimoValido();

        PerfilNinoResponse primero = service.registrarPerfil(request, "trabajador-social-1");
        PerfilNinoResponse segundo = service.registrarPerfil(request, "trabajador-social-1");

        assertEquals("NNA-001", primero.getCodigoCaso());
        assertEquals("NNA-002", segundo.getCodigoCaso());
    }

    @Test
    void marcaComoIncompletoUnPerfilConCamposOpcionalesVacios() {
        PerfilNinoRequest request = new PerfilNinoRequest();
        request.setEdad(8);
        request.setEscolaridad("3er grado");
        request.setNecesidades("Terapia de lenguaje");
        // el resto de los campos queda sin llenar

        PerfilNinoResponse response = service.registrarPerfil(request, "trabajador-social-1");

        assertFalse(response.isPerfilCompleto());
        assertTrue(response.getMensaje().contains("incompleto"));
    }

    private PerfilNinoRequest perfilMinimoValido() {
        PerfilNinoRequest request = new PerfilNinoRequest();
        request.setEdad(9);
        request.setEscolaridad("4to grado");
        request.setNecesidades("Ninguna necesidad especial reportada");
        return request;
    }
}
