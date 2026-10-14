package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import com.senniaf.match.repository.DecisionComiteRepository;
import com.senniaf.match.model.DecisionComite;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilFamiliaService {

    private final PerfilFamiliaRepository repositorio;
    private final DecisionComiteRepository decisiones;

    public PerfilFamiliaService(PerfilFamiliaRepository repositorio, DecisionComiteRepository decisiones) {
        this.repositorio = repositorio;
        this.decisiones = decisiones;
    }

    public synchronized PerfilFamiliaResponse registrarPerfil(PerfilFamiliaRequest request, String registradoPor) {
        String cedula = registradoPor == null ? "" : registradoPor.trim();

        if (cedula.isBlank()) {
            throw new IllegalArgumentException("La cédula del responsable es obligatoria");
        }

        if (repositorio.existsByRegistradoPor(cedula)) {
            throw new IllegalStateException(
                    "Ya existe un caso de familia registrado con la cédula " + cedula);
        }

        String codigoCaso = siguienteCodigoCaso();

        PerfilFamilia perfil = new PerfilFamilia();
        perfil.setCodigoCaso(codigoCaso);
        perfil.setTipoFamilia(request.getTipoFamilia());
        perfil.setNotaEstructuraHogar(request.getNotaEstructuraHogar());
        perfil.setRedApoyo(request.getRedApoyo());
        perfil.setNotaRedApoyo(request.getNotaRedApoyo());
        perfil.setProvincia(request.getProvincia());
        perfil.setDisponibilidadViaje(request.getDisponibilidadViaje());
        perfil.setNotaDistancia(request.getNotaDistancia());
        perfil.setEdadMinimaAceptada(request.getEdadMinimaAceptada());
        perfil.setEdadMaximaAceptada(request.getEdadMaximaAceptada());
        perfil.setNotaRangoEdad(request.getNotaRangoEdad());
        perfil.setDisposicionHermanos(request.getDisposicionHermanos());
        perfil.setNotaHermanos(request.getNotaHermanos());
        perfil.setCondicionesPuedeAcompanarHoy(request.getCondicionesPuedeAcompanarHoy());
        perfil.setCondicionesConApoyo(request.getCondicionesConApoyo());
        perfil.setNotaCondicionesSalud(request.getNotaCondicionesSalud());
        perfil.setContinuidadCultural(request.getContinuidadCultural());
        perfil.setRegistradoPor(cedula);
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        repositorio.save(perfil);

        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";

        return new PerfilFamiliaResponse(mensaje, codigoCaso, completo);
    }

    /** Estado privado de la cuenta autenticada: se obtiene de las decisiones persistidas.
     *  Nunca se entrega a la familia un código de niño, puntaje ni motivo interno.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<PerfilFamiliaResponse> consultarPorCedula(String cedula) {
        return repositorio.findByRegistradoPor(cedula == null ? "" : cedula.trim())
                .map(p -> {
                    String estado = p.isPerfilCompleto() ? "EN_ESPERA" : "INCOMPLETA";
                    java.time.LocalDateTime fechaAceptacion = null;
                    java.util.Optional<DecisionComite> aceptada = decisiones
                            .findFirstByCodigoFamiliaAndDecisionOrderByFechaDescIdDesc(p.getCodigoCaso(), "ACEPTADA");
                    if (aceptada.isPresent()) {
                        estado = "ACEPTADA";
                        fechaAceptacion = aceptada.get().getFecha();
                    } else if (p.isPerfilCompleto()) {
                        // La reevaluación no es un rechazo de la solicitud familiar.
                        boolean reevaluar = decisiones.findFirstByCodigoFamiliaOrderByFechaDescIdDesc(p.getCodigoCaso())
                                .map(d -> "REEVALUAR".equals(d.getDecision())).orElse(false);
                        if (reevaluar) estado = "EN_REVISION";
                    }
                    return new PerfilFamiliaResponse("Perfil encontrado", p.getCodigoCaso(),
                            p.isPerfilCompleto(), estado, fechaAceptacion);
                });
    }

    private synchronized String siguienteCodigoCaso() {
        return "FAM-" + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase(java.util.Locale.ROOT);
    }

    private boolean calcularSiEstaCompleto(PerfilFamiliaRequest r) {
        boolean rangoEdadValido = r.getEdadMinimaAceptada() != null
                && r.getEdadMaximaAceptada() != null
                && r.getEdadMinimaAceptada() <= r.getEdadMaximaAceptada();

        return r.getTipoFamilia() != null
                && noVacia(r.getRedApoyo())
                && r.getProvincia() != null
                && r.getDisponibilidadViaje() != null
                && rangoEdadValido
                && r.getDisposicionHermanos() != null;
    }

    private boolean noVacia(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }
}
