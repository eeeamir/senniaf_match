package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.model.enums.Alergia;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.RespuestaSiNoNoSabe;
import com.senniaf.match.repository.PerfilNinoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilNinoService {

    private final PerfilNinoRepository repositorio;

    public PerfilNinoService(PerfilNinoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public PerfilNinoResponse registrarPerfil(PerfilNinoRequest request, String registradoPor) {
        String codigoCaso = repositorio.siguienteCodigoCaso();

        PerfilNino perfil = new PerfilNino();
        perfil.setCodigoCaso(codigoCaso);
        perfil.setEdad(request.getEdad());
        perfil.setNivelEscolar(request.getNivelEscolar());
        perfil.setGradoEscolar(request.getGradoEscolar());
        perfil.setRendimientoAcademico(request.getRendimientoAcademico());
        perfil.setHabilidades(request.getHabilidades());
        perfil.setHabilidadesDetalle(request.getHabilidadesDetalle());
        perfil.setGustos(request.getGustos());
        perfil.setGustosDetalle(request.getGustosDetalle());
        perfil.setPersonalidad(request.getPersonalidad());
        perfil.setPersonalidadDetalle(request.getPersonalidadDetalle());
        perfil.setAlergias(request.getAlergias());
        perfil.setAlergiasDetalle(request.getAlergiasDetalle());
        perfil.setIntoleranciaLactosa(request.getIntoleranciaLactosa());
        perfil.setCondicionesSalud(request.getCondicionesSalud());
        perfil.setCondicionesSaludDetalle(request.getCondicionesSaludDetalle());
        perfil.setNecesidadesApoyo(request.getNecesidadesApoyo());
        perfil.setTieneHermanos(request.getTieneHermanos());
        perfil.setCantidadHermanos(request.getCantidadHermanos());
        perfil.setHermanosDetalle(request.getHermanosDetalle());
        perfil.setContextoCultural(request.getContextoCultural());
        perfil.setRegistradoPor(registradoPor);
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        boolean alertaMedica = calcularAlertaMedica(request);
        perfil.setAlertaMedica(alertaMedica);

        repositorio.guardar(perfil);

        // Comportamiento esperado según 2.4: un perfil incompleto no se
        // rechaza, pero queda marcado para que el motor de scoring lo
        // excluya del ranking hasta que se complete.
        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";

        if (alertaMedica) {
            mensaje += ". El perfil declara alergias, condiciones de salud o intolerancias "
                    + "que el Comité debe revisar con atención.";
        }

        return new PerfilNinoResponse(mensaje, codigoCaso, completo, alertaMedica);
    }

    private boolean calcularSiEstaCompleto(PerfilNinoRequest r) {
        return r.getNivelEscolar() != null
                && r.getRendimientoAcademico() != null
                && noVacia(r.getHabilidades())
                && noVacia(r.getGustos())
                && noVacia(r.getPersonalidad())
                && noVacia(r.getAlergias())
                && r.getIntoleranciaLactosa() != null
                && noVacia(r.getCondicionesSalud())
                && noVacio(r.getNecesidadesApoyo())
                && r.getTieneHermanos() != null;
    }

    /**
     * Un perfil requiere atención médica particular del Comité cuando
     * declara alguna alergia o condición de salud distinta de "ninguna
     * conocida", o cuando la intolerancia a la lactosa es "SI". No es un
     * criterio de exclusión; es solo una señal de revisión prioritaria.
     */
    private boolean calcularAlertaMedica(PerfilNinoRequest r) {
        boolean tieneAlergiaRelevante = tieneValorDistintoDe(r.getAlergias(), Alergia.NINGUNA_CONOCIDA);
        boolean tieneCondicionRelevante = tieneValorDistintoDe(r.getCondicionesSalud(), CondicionSalud.NINGUNA_CONOCIDA);
        boolean intoleranteALactosa = r.getIntoleranciaLactosa() == RespuestaSiNoNoSabe.SI;
        return tieneAlergiaRelevante || tieneCondicionRelevante || intoleranteALactosa;
    }

    private <T> boolean tieneValorDistintoDe(List<T> lista, T valorNeutro) {
        return lista != null && lista.stream().anyMatch(valor -> valor != valorNeutro);
    }

    private boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean noVacia(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }
}
