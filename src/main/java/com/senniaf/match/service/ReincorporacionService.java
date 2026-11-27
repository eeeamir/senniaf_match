package com.senniaf.match.service;

import com.senniaf.match.model.EventoCaso;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.model.SolicitudReincorporacion;
import com.senniaf.match.model.Vinculacion;
import com.senniaf.match.repository.EventoCasoRepository;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import com.senniaf.match.repository.SolicitudReincorporacionRepository;
import com.senniaf.match.repository.VinculacionRepository;
import com.senniaf.match.security.RolNoAutorizadoException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.senniaf.match.model.Vinculacion.ResultadoCierre.ADOPCION_FORMALIZADA;

@Service
@Transactional(readOnly = true)
public class ReincorporacionService {
    private final VinculacionRepository vinculaciones;
    private final PerfilFamiliaRepository familias;
    private final SolicitudReincorporacionRepository solicitudes;
    private final EventoCasoRepository auditoria;

    public ReincorporacionService(VinculacionRepository vinculaciones, PerfilFamiliaRepository familias,
                                   SolicitudReincorporacionRepository solicitudes, EventoCasoRepository auditoria) {
        this.vinculaciones = vinculaciones;
        this.familias = familias;
        this.solicitudes = solicitudes;
        this.auditoria = auditoria;
    }

    public Map<String, Object> estado(Vinculacion v) {
        if (v.getResultadoCierre() != ADOPCION_FORMALIZADA) return null;
        var solicitud = solicitudes.findByVinculacionId(v.getId());
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("estado", solicitud.map(this::estado).orElse("SIN_SOLICITUD"));
        resultado.put("puedeSolicitar", solicitud.isEmpty() && habilitada(v, v.getFamilia())
                && v.getFamilia().isPerfilCompleto() && !v.getFamilia().isReevaluacionPendiente());
        resultado.put("disponible", solicitud.map(s -> Boolean.TRUE.equals(s.getAutorizada())).orElse(false)
                && v.isFamiliaLiberada() && v.getFamilia().isPerfilCompleto() && !v.getFamilia().isReevaluacionPendiente()
                && !vinculaciones.otraReservaFamiliar(v.getFamilia().getId(), v.getId()));
        return resultado;
    }

    @Transactional
    public void solicitar(Long id, String actor, boolean confirmacion) {
        if (!confirmacion) throw new IllegalArgumentException("Confirma la solicitud de reincorporación");
        Vinculacion v = vinculaciones.bloquear(id).orElseThrow(this::sinAcceso);
        PerfilFamilia familia = familias.bloquearPorRegistradoPor(actor).orElseThrow(this::sinAcceso);
        if (!familia.getId().equals(v.getFamilia().getId())) throw sinAcceso();
        if (!habilitada(v, familia) || !familia.isPerfilCompleto() || familia.isReevaluacionPendiente())
            throw new IllegalStateException("La familia no reúne las condiciones para solicitar reincorporación");
        if (solicitudes.findByVinculacionId(id).isPresent())
            throw new IllegalStateException("Ya existe una solicitud de reincorporación para este cierre");
        solicitudes.save(new SolicitudReincorporacion(v));
        auditar(familia, actor, "REINCORPORACION_SOLICITADA");
    }

    /** Se ejecuta dentro de la misma transacción que guarda la entrevista y bloquea la familia. */
    void confirmarEntrevista(PerfilFamilia familia, Long id, boolean confirmacion) {
        if (!confirmacion) throw new IllegalArgumentException("Confirma la información de tu entrevista");
        var s = solicitudes.findByVinculacionId(id).orElseThrow(this::sinAcceso);
        var v = s.getVinculacion();
        if (!familia.getId().equals(v.getFamilia().getId())) throw sinAcceso();
        if (!habilitada(v, familia) || s.getEntrevistaActualizada() != null || s.getResuelta() != null
                || !familia.isPerfilCompleto() || familia.isReevaluacionPendiente())
            throw new IllegalStateException("No hay una solicitud habilitada para confirmar esta entrevista completa");
        s.confirmarEntrevista();
        auditar(familia, familia.getRegistradoPor(), "REINCORPORACION_ENTREVISTA_CONFIRMADA");
    }

    public List<Map<String, Object>> listar() {
        return solicitudes.findAllByOrderBySolicitadaDescIdDesc().stream().map(s -> {
            var v = s.getVinculacion();
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", s.getId());
            dto.put("codigoCaso", v.getFamilia().getCodigoCaso());
            dto.put("vinculacion", "VIN-" + v.getId());
            dto.put("fechaSolicitud", fecha(s.getSolicitada()));
            dto.put("fechaResolucion", fecha(s.getResuelta()));
            dto.put("motivoResolucion", s.getFundamento());
            dto.put("resueltoPor", s.getResueltoPor());
            dto.put("estado", estado(s));
            return dto;
        }).toList();
    }

    @Transactional
    public void resolver(Long id, String actor, boolean autorizar, String fundamento, boolean confirmacion) {
        if (!confirmacion || fundamento == null || fundamento.strip().length() < 10 || fundamento.length() > 2000)
            throw new IllegalArgumentException("Confirma la revisión y documenta la decisión en 10 a 2000 caracteres");
        var vinculacionId = solicitudes.vinculacionId(id).orElseThrow(this::sinAcceso);
        var v = vinculaciones.bloquear(vinculacionId).orElseThrow(this::sinAcceso);
        var familia = familias.bloquearPorId(v.getFamilia().getId()).orElseThrow(this::sinAcceso);
        // La referencia anterior pudo leerse mientras otra actuación estaba en curso.
        var s = solicitudes.bloquear(id).orElseThrow(this::sinAcceso);
        if (s.getResuelta() != null || s.getEntrevistaActualizada() == null)
            throw new IllegalStateException("La solicitud debe estar pendiente y tener la entrevista confirmada");
        if (autorizar && (!habilitada(v, familia) || !familia.isPerfilCompleto() || familia.isReevaluacionPendiente()))
            throw new IllegalStateException("El expediente familiar no reúne las condiciones vigentes para volver al Match");
        s.resolver(autorizar, actor, fundamento.strip());
        if (autorizar) v.setFamiliaLiberada(true);
        auditar(familia, actor, autorizar ? "REINCORPORACION_AUTORIZADA" : "REINCORPORACION_NO_AUTORIZADA");
    }

    private boolean habilitada(Vinculacion v, PerfilFamilia familia) {
        return v.getResultadoCierre() == ADOPCION_FORMALIZADA && v.getEstado() == Vinculacion.Estado.CERRADA
                && v.isReservaVigente() && !v.isFamiliaLiberada()
                && !vinculaciones.otraReservaFamiliar(familia.getId(), v.getId())
                && vinculaciones.findFirstByFamiliaIdAndResultadoCierreOrderByFechaRegistroCierreDescIdDesc(
                        familia.getId(), ADOPCION_FORMALIZADA).map(ultima -> ultima.getId().equals(v.getId())).orElse(false);
    }

    private String estado(SolicitudReincorporacion s) {
        if (s.getAutorizada() != null) return s.getAutorizada() ? "AUTORIZADA" : "NO_AUTORIZADA";
        return s.getEntrevistaActualizada() == null ? "ENTREVISTA_PENDIENTE" : "REVISION_PENDIENTE";
    }

    private Object fecha(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private RolNoAutorizadoException sinAcceso() {
        return new RolNoAutorizadoException("La solicitud de reincorporación no está disponible");
    }

    private void auditar(PerfilFamilia familia, String actor, String accion) {
        EventoCaso evento = new EventoCaso();
        evento.setCodigoCaso(familia.getCodigoCaso());
        evento.setActor(actor);
        evento.setAccion(accion);
        evento.setFecha(LocalDateTime.now());
        evento.setDescripcion("Actuación de reincorporación familiar registrada.");
        auditoria.save(evento);
    }
}
