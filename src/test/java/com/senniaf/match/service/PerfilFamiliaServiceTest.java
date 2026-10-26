package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.DisponibilidadViaje;
import com.senniaf.match.model.enums.DisposicionHermanos;
import com.senniaf.match.model.enums.FuenteApoyo;
import com.senniaf.match.model.enums.ProvinciaPanama;
import com.senniaf.match.model.enums.TipoFamilia;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilFamiliaServiceTest {

    private PerfilFamiliaService service;

    @BeforeEach
    void setUp() {
        PerfilFamiliaRepository repo = mock(PerfilFamiliaRepository.class);
        when(repo.count()).thenReturn(0L);
        when(repo.save(any(PerfilFamilia.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.existsByRegistradoPor(anyString())).thenReturn(false);
        service = new PerfilFamiliaService(repo);
    }

    @Test
    void registraUnPerfilCompletoYDevuelveCodigoDeCaso() {
        PerfilFamiliaRequest request = perfilCompleto();

        PerfilFamiliaResponse response = service.registrarPerfil(request, "familia-1");

        assertEquals("FAM-001", response.getCodigoCaso());
        assertEquals("Perfil registrado correctamente", response.getMensaje());
        assertTrue(response.isPerfilCompleto());
    }

    @Test
    void impideRegistrarDosCasosConLaMismaCedula() {
        PerfilFamiliaRequest request = perfilCompleto();
        PerfilFamiliaRepository repo = mock(PerfilFamiliaRepository.class);
        when(repo.count()).thenReturn(0L);
        when(repo.save(any(PerfilFamilia.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.existsByRegistradoPor("8-123-456")).thenReturn(false, true);
        service = new PerfilFamiliaService(repo);

        PerfilFamiliaResponse primero = service.registrarPerfil(request, "8-123-456");

        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> service.registrarPerfil(request, "8-123-456"));

        assertEquals("FAM-001", primero.getCodigoCaso());
        assertTrue(excepcion.getMessage().contains("8-123-456"));
    }

    @Test
    void marcaComoIncompletoUnPerfilSinCamposObligatorios() {
        PerfilFamiliaRequest request = new PerfilFamiliaRequest();
        request.setTipoFamilia(TipoFamilia.PAREJA);
        // el resto de los campos obligatorios queda sin llenar

        PerfilFamiliaResponse response = service.registrarPerfil(request, "familia-1");

        assertFalse(response.isPerfilCompleto());
        assertTrue(response.getMensaje().contains("incompleto"));
    }

    @Test
    void marcaComoIncompletoUnPerfilConRangoDeEdadInconsistente() {
        PerfilFamiliaRequest request = perfilCompleto();
        request.setEdadMinimaAceptada(10);
        request.setEdadMaximaAceptada(5);

        PerfilFamiliaResponse response = service.registrarPerfil(request, "familia-1");

        assertFalse(response.isPerfilCompleto());
    }

    @Test
    void permiteListasDeCondicionesDeSaludVaciasSinAfectarLaCompletitud() {
        PerfilFamiliaRequest request = perfilCompleto();
        request.setCondicionesPuedeAcompanarHoy(List.of());
        request.setCondicionesConApoyo(List.of());

        PerfilFamiliaResponse response = service.registrarPerfil(request, "familia-1");

        assertTrue(response.isPerfilCompleto());
    }

    @Test
    void cuentaLaReevaluacionSoloAlEnviarUnaCorreccionCompleta() {
        PerfilFamiliaRepository repo = mock(PerfilFamiliaRepository.class);
        PerfilFamilia perfil = new PerfilFamilia();
        perfil.setCodigoCaso("FAM-001");
        perfil.setReevaluacionPendiente(true);
        when(repo.findByRegistradoPor("8-123-456")).thenReturn(Optional.of(perfil));
        when(repo.save(any(PerfilFamilia.class))).thenAnswer(i -> i.getArgument(0));
        PerfilFamiliaService servicio = new PerfilFamiliaService(repo);

        // Abrir el formulario y consultar el perfil no ejecuta actualizarPropio.
        assertEquals(0, perfil.getNumeroReevaluaciones());
        assertTrue(perfil.isReevaluacionPendiente());

        PerfilFamiliaResponse primera = servicio.actualizarPropio(perfilCompleto(), "8-123-456");
        assertEquals(1, primera.getNumeroReevaluaciones());
        assertFalse(primera.isReevaluacionPendiente());

        // Un nuevo PUT sin otra solicitud del Comité no suma un intento adicional.
        PerfilFamiliaResponse segunda = servicio.actualizarPropio(perfilCompleto(), "8-123-456");
        assertEquals(1, segunda.getNumeroReevaluaciones());
        assertFalse(segunda.isReevaluacionPendiente());
    }

    @Test
    void mantienePendienteUnaCorreccionIncompletaSinContarla() {
        PerfilFamiliaRepository repo = mock(PerfilFamiliaRepository.class);
        PerfilFamilia perfil = new PerfilFamilia();
        perfil.setCodigoCaso("FAM-002");
        perfil.setReevaluacionPendiente(true);
        when(repo.findByRegistradoPor("8-456-789")).thenReturn(Optional.of(perfil));
        PerfilFamiliaService servicio = new PerfilFamiliaService(repo);

        PerfilFamiliaRequest incompleta = perfilCompleto();
        incompleta.setEdadMinimaAceptada(12);
        incompleta.setEdadMaximaAceptada(7);

        PerfilFamiliaResponse respuesta = servicio.actualizarPropio(incompleta, "8-456-789");
        assertEquals(0, respuesta.getNumeroReevaluaciones());
        assertTrue(respuesta.isReevaluacionPendiente());
        assertFalse(respuesta.isPerfilCompleto());
    }

    private PerfilFamiliaRequest perfilCompleto() {
        PerfilFamiliaRequest request = new PerfilFamiliaRequest();
        request.setTipoFamilia(TipoFamilia.PAREJA);
        request.setNotaEstructuraHogar("Vivimos juntos hace 8 años, sin otros hijos");
        request.setRedApoyo(List.of(FuenteApoyo.PAREJA, FuenteApoyo.FAMILIA_EXTENDIDA));
        request.setNotaRedApoyo("Mis padres viven cerca y podrían ayudar");
        request.setProvincia(ProvinciaPanama.PANAMA);
        request.setDisponibilidadViaje(DisponibilidadViaje.ALTA);
        request.setNotaDistancia("Podemos viajar cada fin de semana si es necesario");
        request.setEdadMinimaAceptada(3);
        request.setEdadMaximaAceptada(8);
        request.setNotaRangoEdad("Nos sentimos más preparados para edad escolar");
        request.setDisposicionHermanos(DisposicionHermanos.DISPUESTA_CON_DUDAS);
        request.setNotaHermanos("Nos preocupa el espacio en la casa para dos");
        request.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA));
        request.setCondicionesConApoyo(List.of(CondicionSalud.TDAH));
        request.setNotaCondicionesSalud("Nos gustaría capacitación sobre TDAH");
        request.setContinuidadCultural("Tenemos raíces indígenas y nos gustaría preservarlas");
        return request;
    }
}
