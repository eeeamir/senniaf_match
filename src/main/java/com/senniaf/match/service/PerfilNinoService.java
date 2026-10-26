package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.model.EventoCaso;
import com.senniaf.match.model.enums.Alergia;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.RespuestaSiNoNoSabe;
import com.senniaf.match.repository.PerfilNinoRepository;
import com.senniaf.match.repository.EventoCasoRepository;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
public class PerfilNinoService {

    private final PerfilNinoRepository repositorio;
    private final EventoCasoRepository auditoria;

    public PerfilNinoService(PerfilNinoRepository repositorio) {
        this(repositorio, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PerfilNinoService(PerfilNinoRepository repositorio, EventoCasoRepository auditoria) {
        this.repositorio = repositorio; this.auditoria = auditoria;
    }

    public PerfilNinoResponse registrarPerfil(PerfilNinoRequest request, String registradoPor) {
        String codigoCaso = siguienteCodigoCaso();

        PerfilNino perfil = new PerfilNino();
        perfil.setCodigoCaso(codigoCaso);
        copiarDatos(perfil, request);
        perfil.setRegistradoPor(registradoPor);
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        boolean alertaMedica = calcularAlertaMedica(request);
        perfil.setAlertaMedica(alertaMedica);

        repositorio.save(perfil);
        auditar(codigoCaso, registradoPor, "CREACION");

        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";
        if (alertaMedica) mensaje += ". Este perfil requiere atención y revisión médica por el Comité.";
        return new PerfilNinoResponse(mensaje, codigoCaso, completo, alertaMedica);
    }

    private void copiarDatos(PerfilNino perfil, PerfilNinoRequest request) {
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
    }

    public List<PerfilNino> listarPerfiles(String actor) {
    return repositorio.findAll().stream()
            .filter(p -> !p.isBajaLogica() && actor.equals(p.getRegistradoPor()))
            .sorted(Comparator.comparing(
                    PerfilNino::getFechaRegistro,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
}

    public PerfilNino propio(String codigo, String actor) {
        PerfilNino p = repositorio.findByCodigoCaso(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El caso no existe"));
        if (p.isBajaLogica() || !actor.equals(p.getRegistradoPor()))
            throw new RolNoAutorizadoException("No puedes consultar o modificar un caso ajeno o dado de baja");
        return p;
    }

    public PerfilNinoResponse actualizar(String codigo, String actor, PerfilNinoRequest request) {
        PerfilNino p = propio(codigo, actor);
        copiarDatos(p, request);
        p.setPerfilCompleto(calcularSiEstaCompleto(request));
        p.setAlertaMedica(calcularAlertaMedica(request));
        repositorio.save(p);
        auditar(codigo, actor, "EDICION");
        return new PerfilNinoResponse("Perfil actualizado correctamente", codigo, p.isPerfilCompleto(), p.isAlertaMedica());
    }

    public void darDeBaja(String codigo, String actor) {
        PerfilNino p = propio(codigo, actor);
        p.setBajaLogica(true);
        repositorio.save(p);
        auditar(codigo, actor, "BAJA_LOGICA");
    }

    public List<EventoCaso> historial(String codigo, String actor) {
        propio(codigo, actor);
        return auditoria == null ? List.of() : auditoria.findByCodigoCasoOrderByFechaAsc(codigo);
    }

    private void auditar(String codigo, String actor, String accion) {
        if (auditoria == null) return; // soporte a pruebas unitarias antiguas sin repositorio de auditoría
        EventoCaso e = new EventoCaso(); e.setCodigoCaso(codigo); e.setActor(actor);
        e.setAccion(accion); e.setFecha(LocalDateTime.now()); auditoria.save(e);
    }

    private synchronized String siguienteCodigoCaso() {
        return String.format("NNA-%03d", repositorio.count() + 1);
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
