package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilFamiliaServiceTest {

    private PerfilFamiliaService service;

    @BeforeEach
    void setUp() {
        service = new PerfilFamiliaService(new PerfilFamiliaRepository());
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
    void generaCodigosDeCasoSecuencialesParaCadaPerfil() {
        PerfilFamiliaRequest request = perfilCompleto();

        PerfilFamiliaResponse primero = service.registrarPerfil(request, "familia-1");
        PerfilFamiliaResponse segundo = service.registrarPerfil(request, "familia-1");

        assertEquals("FAM-001", primero.getCodigoCaso());
        assertEquals("FAM-002", segundo.getCodigoCaso());
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
