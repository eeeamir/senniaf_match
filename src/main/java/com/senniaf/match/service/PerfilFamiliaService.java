package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilFamiliaService {

    private final PerfilFamiliaRepository repositorio;

    public PerfilFamiliaService(PerfilFamiliaRepository repositorio) {
        this.repositorio = repositorio;
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
        copiarDatos(perfil, request);
        perfil.setRegistradoPor(cedula);
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        repositorio.save(perfil);

        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";

        return respuesta(perfil, mensaje);
    }

    /** La familia corrige su propia entrevista sin crear una segunda ficha ni borrar el historial. */
    @Transactional
    public PerfilFamiliaResponse actualizarPropio(PerfilFamiliaRequest request, String cedula) {
        PerfilFamilia perfil = repositorio.findByRegistradoPor(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Todavía no existe una entrevista para modificar"));
        copiarDatos(perfil, request);
        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);
        // Abrir o abandonar el formulario nunca cuenta. Una solicitud pendiente
        // cuenta una sola vez al recibirse la corrección completa y válida.
        if (perfil.isReevaluacionPendiente() && completo) {
            perfil.setNumeroReevaluaciones(perfil.getNumeroReevaluaciones() + 1);
            perfil.setReevaluacionPendiente(false);
        }
        repositorio.save(perfil);
        return respuesta(perfil, perfil.isPerfilCompleto() ? "Entrevista actualizada para revisión del Comité"
                : "Los cambios se guardaron, pero la entrevista sigue incompleta y requiere corrección");
    }

    public java.util.Optional<PerfilFamilia> consultarDetallePropio(String cedula) {
        return repositorio.findByRegistradoPor(cedula == null ? "" : cedula.trim());
    }

    private PerfilFamiliaResponse respuesta(PerfilFamilia p, String mensaje) {
        return new PerfilFamiliaResponse(mensaje, p.getCodigoCaso(), p.isPerfilCompleto(),
                p.isReevaluacionPendiente(), p.getNumeroReevaluaciones());
    }

    private void copiarDatos(PerfilFamilia perfil, PerfilFamiliaRequest request) {
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
    }

    /** Perfil que la familia ya registró con su cédula, si existe. */
    public java.util.Optional<PerfilFamiliaResponse> consultarPorCedula(String cedula) {
        return repositorio.findByRegistradoPor(cedula == null ? "" : cedula.trim())
                .map(p -> respuesta(p, "Perfil encontrado"));
    }

    private synchronized String siguienteCodigoCaso() {
        return String.format("FAM-%03d", repositorio.count() + 1);
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
