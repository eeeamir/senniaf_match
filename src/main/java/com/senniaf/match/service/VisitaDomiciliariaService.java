package com.senniaf.match.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.senniaf.match.model.*;
import com.senniaf.match.model.EventoVisitaDomiciliaria.Accion;
import com.senniaf.match.repository.*;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.SeguimientoVinculacionService.Cambio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

import static com.senniaf.match.service.TiemposVinculacion.PANAMA;

@Service
@Transactional(readOnly = true)
public class VisitaDomiciliariaService {
    private final SeguimientoVinculacionService seguimientos;
    private final VinculacionService vinculaciones;
    private final InterrupcionConvivenciaRepository interrupciones;
    private final VisitaDomiciliariaRepository visitas;
    private final EventoVisitaDomiciliariaRepository historial;

    public VisitaDomiciliariaService(SeguimientoVinculacionService seguimientos, VinculacionService vinculaciones,
                                     InterrupcionConvivenciaRepository interrupciones, VisitaDomiciliariaRepository visitas,
                                     EventoVisitaDomiciliariaRepository historial) {
        this.seguimientos = seguimientos;
        this.vinculaciones = vinculaciones;
        this.interrupciones = interrupciones;
        this.visitas = visitas;
        this.historial = historial;
    }

    public record Solicitud(LocalDate fecha, LocalTime inicio, LocalTime fin, UUID claveRegistro,
                            Long version, String motivoModificacion) {}
    public record Registro(Long id, LocalDate fecha, LocalTime inicio, LocalTime fin, long minutos,
                           long version, boolean puedeEditar) {}
    public record Movimiento(Long visitaId, String responsable, Instant fecha, Accion accion,
                             Map<String, Cambio> cambios, String motivo) {}
    public record Consulta(List<Registro> registros, List<Movimiento> historial, LocalDate fechaMinima,
                           int referencia, boolean puedeRegistrar) {}

    public Consulta consultar(Long id, Usuario usuario) {
        Vinculacion v = seguimientos.autorizada(id, usuario, false);
        boolean editable = profesional(usuario) && permiteCambios(v);
        return new Consulta(visitas.findByVinculacionIdOrderByFechaAscInicioAscIdAsc(id).stream()
                .map(visita -> vista(visita, usuario, editable)).toList(),
                historial.findByVisitaVinculacionIdOrderByFechaDescIdDesc(id).stream()
                .map(evento -> new Movimiento(evento.getVisitaId(), evento.getNombreActor(), evento.getFecha(),
                        evento.getAccion(), seguimientos.leer(evento.getCambios(), new TypeReference<>() {}),
                        evento.getMotivo())).toList(),
                v.getFechaInicioConvivencia(), TiemposVinculacion.VISITAS_REFERENCIA_ACOGIMIENTO, editable);
    }

    @Transactional
    public Registro registrar(Long id, Usuario usuario, Solicitud datos) {
        Vinculacion v = bloquear(id, usuario);
        if (datos == null || datos.claveRegistro() == null) {
            throw new IllegalArgumentException("Falta la clave del envío. Vuelve a abrir el formulario.");
        }
        var previa = visitas.findByVinculacionIdAndAutorAndClaveRegistro(id, usuario.getIdentificador(), datos.claveRegistro());
        if (previa.isPresent()) {
            var visita = previa.get();
            if (!campos(visita.getFecha(), visita.getInicio(), visita.getFin()).equals(campos(datos.fecha(), datos.inicio(), datos.fin()))) {
                throw new IllegalStateException("Ese envío ya registró una visita. Actualiza la consulta antes de continuar.");
            }
            return vista(visita, usuario, permiteCambios(v));
        }
        Instant ahora = Instant.now();
        validar(v, datos, ahora, 0L);
        var visita = new VisitaDomiciliaria(id, usuario, ahora, datos.claveRegistro());
        visita.actualizar(datos.fecha(), datos.inicio(), datos.fin());
        visitas.saveAndFlush(visita);
        auditar(visita, usuario, ahora, Accion.REGISTRADA,
                SeguimientoVinculacionService.comparar(Map.of(), campos(datos.fecha(), datos.inicio(), datos.fin())), null);
        return vista(visita, usuario, true);
    }

    @Transactional
    public Registro editar(Long id, Long visitaId, Usuario usuario, Solicitud datos) {
        Vinculacion v = bloquear(id, usuario);
        var visita = visitas.findByVinculacionIdAndId(id, visitaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la visita en esta vinculación"));
        if (!autor(visita, usuario)) throw new RolNoAutorizadoException("Solo puedes editar las visitas que registraste");
        if (datos == null || datos.version() == null) {
            throw new IllegalArgumentException("Falta la revisión de la visita. Vuelve a abrir el formulario.");
        }
        Instant ahora = Instant.now();
        validar(v, datos, ahora, visitaId);
        var cambios = SeguimientoVinculacionService.comparar(campos(visita.getFecha(), visita.getInicio(), visita.getFin()),
                campos(datos.fecha(), datos.inicio(), datos.fin()));
        if (cambios.isEmpty()) return vista(visita, usuario, true);
        if (datos.version() != visita.getVersion()) {
            throw new IllegalStateException("La visita cambió mientras la editabas. Actualiza la consulta.");
        }
        String motivo = SeguimientoVinculacionService.texto(datos.motivoModificacion(), "El motivo de modificación", 1000, false);
        visita.actualizar(datos.fecha(), datos.inicio(), datos.fin());
        visitas.flush();
        auditar(visita, usuario, ahora, Accion.MODIFICADA, cambios, motivo);
        return vista(visita, usuario, true);
    }

    private Vinculacion bloquear(Long id, Usuario usuario) {
        if (!profesional(usuario)) throw new RolNoAutorizadoException("Solo el equipo técnico asignado puede registrar o editar visitas");
        return seguimientos.autorizada(id, usuario, true);
    }

    private boolean profesional(Usuario usuario) {
        return usuario != null && ("TRABAJADOR_SOCIAL".equals(usuario.getRol()) || "PSICOLOGO".equals(usuario.getRol()));
    }

    private boolean autor(VisitaDomiciliaria visita, Usuario usuario) {
        return visita.getAutor().equals(usuario.getIdentificador()) && visita.getRol().equals(usuario.getRol());
    }

    private boolean permiteCambios(Vinculacion v) {
        return vinculaciones.reportable(v) && v.getFechaEmisionAutorizacion() != null
                && v.getFechaRegistroInicio() != null && v.getFechaCierre() == null
                && !interrupciones.existsByVinculacionId(v.getId());
    }

    private void validar(Vinculacion v, Solicitud datos, Instant ahora, Long excluir) {
        if (!permiteCambios(v)) throw new IllegalStateException("La vinculación no permite registrar ni modificar visitas");
        LocalDateTime momento = LocalDateTime.ofInstant(ahora, PANAMA);
        if (datos.fecha() == null) throw new IllegalArgumentException("Indica la fecha de la visita");
        if (datos.fecha().isAfter(momento.toLocalDate())) throw new IllegalArgumentException("No puedes registrar una visita en una fecha futura");
        if (datos.fecha().isBefore(v.getFechaInicioConvivencia())) {
            throw new IllegalArgumentException("La visita no puede ser anterior al inicio de la convivencia");
        }
        if (!horaValida(datos.inicio()) || !horaValida(datos.fin())) {
            throw new IllegalArgumentException("Indica horas de inicio y finalización válidas, en horas y minutos");
        }
        if (!datos.fin().isAfter(datos.inicio())) {
            throw new IllegalArgumentException("La hora de finalización debe ser posterior a la hora de inicio");
        }
        if (datos.fecha().atTime(datos.fin()).isAfter(momento)) {
            throw new IllegalArgumentException("Solo puedes registrar visitas que ya hayan finalizado");
        }
        // Las altas y ediciones de una vinculación comparten el bloqueo de su fila.
        if (visitas.existeSuperposicion(v.getId(), datos.fecha(), datos.inicio(), datos.fin(), excluir)) {
            throw new IllegalStateException("Ya existe una visita registrada que coincide con ese horario");
        }
    }

    private boolean horaValida(LocalTime hora) {
        return hora != null && hora.getSecond() == 0 && hora.getNano() == 0;
    }

    private Map<String, String> campos(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("fecha", Objects.toString(fecha, null));
        campos.put("inicio", Objects.toString(inicio, null));
        campos.put("fin", Objects.toString(fin, null));
        return campos;
    }

    private Registro vista(VisitaDomiciliaria visita, Usuario usuario, boolean editable) {
        return new Registro(visita.getId(), visita.getFecha(), visita.getInicio(), visita.getFin(),
                Duration.between(visita.getInicio(), visita.getFin()).toMinutes(), visita.getVersion(),
                editable && autor(visita, usuario));
    }

    private void auditar(VisitaDomiciliaria visita, Usuario usuario, Instant ahora, Accion accion,
                         Map<String, Cambio> cambios, String motivo) {
        historial.save(new EventoVisitaDomiciliaria(visita, usuario, SeguimientoVinculacionService.nombreCompleto(usuario),
                ahora, accion, seguimientos.escribir(cambios), motivo));
    }
}
