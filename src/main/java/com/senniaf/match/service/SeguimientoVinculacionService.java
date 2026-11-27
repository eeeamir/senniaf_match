package com.senniaf.match.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.model.EventoSeguimientoVinculacion.Accion;
import com.senniaf.match.model.SeguimientoVinculacion.Etapa;
import com.senniaf.match.repository.*;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SeguimientoVinculacionService {
    private static final String SOCIAL = "TRABAJADOR_SOCIAL";
    private static final String PSICOLOGIA = "PSICOLOGO";
    private static final String COMITE = "COMITE";
    private static final ZoneId PANAMA = ZoneId.of("America/Panama");
    private static final Map<String, Set<String>> TIPOS = Map.of(
            SOCIAL, Set.of("VISITA_DOMICILIARIA", "ENTREVISTA_FAMILIAR", "ENCUENTRO", "INTERRELACION", "ORIENTACION_FAMILIAR", "TELEFONICO", "OTRO"),
            PSICOLOGIA, Set.of("SESION_INDIVIDUAL", "SESION_FAMILIAR", "OBSERVACION_VINCULO", "ENTREVISTA",
                    "ENTREVISTA_PSICOLOGICA", "INTERRELACION", "ORIENTACION_FAMILIAR", "VISITA_DOMICILIARIA", "TELEFONICO", "OTRO"));

    private final VinculacionRepository vinculaciones;
    private final VinculacionService accesoVinculacion;
    private final InterrupcionConvivenciaRepository interrupciones;
    private final SeguimientoVinculacionRepository seguimientos;
    private final EventoSeguimientoVinculacionRepository historial;
    private final ObjectMapper json;

    public SeguimientoVinculacionService(VinculacionRepository vinculaciones, VinculacionService accesoVinculacion,
                                         InterrupcionConvivenciaRepository interrupciones,
                                         SeguimientoVinculacionRepository seguimientos,
                                         EventoSeguimientoVinculacionRepository historial, ObjectMapper json) {
        this.vinculaciones = vinculaciones;
        this.accesoVinculacion = accesoVinculacion;
        this.interrupciones = interrupciones;
        this.seguimientos = seguimientos;
        this.historial = historial;
        this.json = json;
    }

    public record Solicitud(LocalDate fechaAtencion, String tipo, String participantes, String adaptacion,
                            String contexto, String dificultades, String recomendaciones, LocalDate proximoSeguimiento,
                            UUID claveRegistro, Long version, String motivoModificacion) {}

    public record Cambio(String anterior, String actual) {}

    public record Registro(Long id, String rol, String profesional, Instant registrado, LocalDate fechaAtencion,
                           Etapa etapa, Map<String, String> contenido, long version, boolean puedeEditar) {}

    public record Movimiento(Long seguimientoId, String rol, String responsable, Instant fecha, Accion accion,
                             Map<String, Cambio> cambios, String motivo) {}

    public record Consulta(List<Registro> registros, List<Movimiento> historial, String rolRegistro,
                           List<String> rolesConsulta, LocalDate primeraFecha, LocalDate hoy, LocalDate inicioConvivencia) {}

    public Consulta consultar(Long id, Usuario usuario) {
        Vinculacion vinculacion = autorizada(id, usuario, false);
        String rol = usuario.getRol();
        var roles = COMITE.equals(rol) ? List.of(SOCIAL, PSICOLOGIA) : List.of(rol);
        boolean permiteCambios = TIPOS.containsKey(rol) && permiteCambios(vinculacion);
        var registros = seguimientos.findByVinculacionIdAndRolInOrderByFechaAtencionDescIdDesc(id, roles).stream()
                .map(registro -> vista(registro, usuario, permiteCambios)).toList();
        var movimientos = historial.findByVinculacionIdAndRolInOrderByFechaDescIdDesc(id, roles).stream()
                .map(evento -> new Movimiento(evento.getSeguimientoId(), evento.getRol(), evento.getNombreActor(),
                        evento.getFecha(), evento.getAccion(), leer(evento.getCambios(), new TypeReference<>() {}),
                        evento.getMotivo())).toList();
        return new Consulta(registros, movimientos, permiteCambios ? rol : null, roles,
                fechaAceptacion(vinculacion), LocalDate.now(PANAMA), inicioConfirmado(vinculacion));
    }

    @Transactional
    public Registro registrar(Long id, Usuario usuario, Solicitud datos) {
        Vinculacion vinculacion = autorizadaParaEscritura(id, usuario);
        if (datos == null || datos.claveRegistro() == null) {
            throw new IllegalArgumentException("Falta el identificador del envío. Vuelve a abrir el formulario.");
        }
        var existente = seguimientos.findByVinculacionIdAndAutorAndClaveRegistro(
                id, usuario.getIdentificador(), datos.claveRegistro());
        if (existente.isPresent()) {
            verificarAutor(existente.get(), usuario);
            return vista(existente.get(), usuario, permiteCambios(vinculacion));
        }
        verificarEstado(vinculacion);
        var contenido = validar(datos, vinculacion, usuario.getRol());
        var etapa = etapa(vinculacion, datos.fechaAtencion());
        Instant ahora = Instant.now();
        var registro = new SeguimientoVinculacion(id, usuario, nombreCompleto(usuario), datos.claveRegistro(), ahora);
        registro.actualizar(datos.fechaAtencion(), etapa, escribir(contenido));
        seguimientos.saveAndFlush(registro);
        guardarEvento(registro, usuario, ahora, Accion.REGISTRADO,
                comparar(Map.of(), campos(datos.fechaAtencion(), etapa, contenido)), null);
        return vista(registro, usuario, true);
    }

    @Transactional
    public Registro editar(Long id, Long seguimientoId, Usuario usuario, Solicitud datos) {
        Vinculacion vinculacion = autorizadaParaEscritura(id, usuario);
        verificarEstado(vinculacion);
        var registro = seguimientos.findByVinculacionIdAndId(id, seguimientoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el seguimiento en esta vinculación"));
        verificarAutor(registro, usuario);
        if (datos == null || datos.version() == null) {
            throw new IllegalArgumentException("Falta la revisión del registro. Vuelve a abrir el formulario.");
        }
        var contenido = validar(datos, vinculacion, usuario.getRol());
        var etapa = registro.getFechaAtencion().equals(datos.fechaAtencion()) ? registro.getEtapa()
                : etapa(vinculacion, datos.fechaAtencion());
        var cambios = comparar(campos(registro.getFechaAtencion(), registro.getEtapa(), contenido(registro)),
                campos(datos.fechaAtencion(), etapa, contenido));
        if (cambios.isEmpty()) return vista(registro, usuario, true);
        if (datos.version() != registro.getVersion()) {
            throw new IllegalStateException("Este seguimiento cambió mientras lo editabas. Actualiza la consulta antes de editarlo de nuevo.");
        }
        String motivo = texto(datos.motivoModificacion(), "El motivo de modificación", 1000, false);
        registro.actualizar(datos.fechaAtencion(), etapa, escribir(contenido));
        seguimientos.flush();
        guardarEvento(registro, usuario, Instant.now(), Accion.MODIFICADO, cambios, motivo);
        return vista(registro, usuario, true);
    }

    private Vinculacion autorizadaParaEscritura(Long id, Usuario usuario) {
        if (!TIPOS.containsKey(rol(usuario))) {
            throw new RolNoAutorizadoException("Solo un profesional asignado puede registrar o editar seguimientos");
        }
        return autorizada(id, usuario, true);
    }

    Vinculacion autorizada(Long id, Usuario usuario, boolean bloquear) {
        String rol = rol(usuario);
        if (!COMITE.equals(rol) && !TIPOS.containsKey(rol)) {
            throw new RolNoAutorizadoException("Los seguimientos son de consulta institucional");
        }
        var vinculacion = (bloquear ? vinculaciones.bloquear(id) : vinculaciones.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("La vinculación no está disponible"));
        if (!accesoVinculacion.autorizado(vinculacion, rol, usuario.getIdentificador())) {
            throw new RolNoAutorizadoException("No tienes asignada esta vinculación");
        }
        return vinculacion;
    }

    private String rol(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) throw new RolNoAutorizadoException("Se requiere una sesión institucional");
        return usuario.getRol();
    }

    private boolean permiteCambios(Vinculacion v) {
        return v.getEstado() == Vinculacion.Estado.RESPUESTA_ACEPTADA && v.getFechaRespuesta() != null
                && v.isReservaVigente() && v.getFechaCierre() == null && !v.getNino().isCierrePorAdopcion()
                && !interrupciones.existsByVinculacionId(v.getId());
    }

    private void verificarEstado(Vinculacion vinculacion) {
        if (!permiteCambios(vinculacion)) {
            throw new IllegalStateException("El estado actual de la vinculación impide registrar o editar seguimientos");
        }
    }

    private boolean autor(SeguimientoVinculacion registro, Usuario usuario) {
        return registro.getAutor().equals(usuario.getIdentificador()) && registro.getRol().equals(usuario.getRol());
    }

    private void verificarAutor(SeguimientoVinculacion registro, Usuario usuario) {
        if (!autor(registro, usuario)) throw new RolNoAutorizadoException("Solo puedes editar tus propios seguimientos");
    }

    private LocalDate fechaAceptacion(Vinculacion vinculacion) {
        var fecha = vinculacion.getFechaRespuesta();
        return fecha == null ? null : fecha.atZone(ZoneId.systemDefault()).withZoneSameInstant(PANAMA).toLocalDate();
    }

    private LocalDate inicioConfirmado(Vinculacion vinculacion) {
        return vinculacion.getFechaRegistroAutorizacion() != null && vinculacion.getFechaRegistroInicio() != null
                ? vinculacion.getFechaInicioConvivencia() : null;
    }

    private Etapa etapa(Vinculacion vinculacion, LocalDate fechaAtencion) {
        LocalDate inicio = inicioConfirmado(vinculacion);
        return inicio != null && !fechaAtencion.isBefore(inicio) ? Etapa.CONVIVENCIA_PREADOPTIVA : Etapa.ENCUENTROS_PREVIOS;
    }

    private Map<String, String> validar(Solicitud datos, Vinculacion vinculacion, String rol) {
        LocalDate fecha = datos.fechaAtencion();
        if (fecha == null || fecha.isBefore(fechaAceptacion(vinculacion)) || fecha.isAfter(LocalDate.now(PANAMA))) {
            throw new IllegalArgumentException("La fecha de atención debe estar entre la aceptación familiar y hoy");
        }
        if (datos.tipo() == null || !TIPOS.get(rol).contains(datos.tipo())) {
            throw new IllegalArgumentException("Selecciona un tipo de seguimiento de tu especialidad");
        }
        if (datos.proximoSeguimiento() != null && datos.proximoSeguimiento().isBefore(fecha)) {
            throw new IllegalArgumentException("El próximo seguimiento no puede ser anterior a la atención registrada");
        }
        Map<String, String> contenido = new LinkedHashMap<>();
        contenido.put("tipo", datos.tipo());
        String participantes = texto(datos.participantes(), "Los participantes", 500, false);
        if (participantes != null) contenido.put("participantes", participantes);
        contenido.put("adaptacion", texto(datos.adaptacion(), "La observación de adaptación", 2000, true));
        contenido.put("contexto", texto(datos.contexto(), "La segunda observación", 2000, true));
        contenido.put("dificultades", texto(datos.dificultades(), "Las dificultades identificadas", 2000, true));
        contenido.put("recomendaciones", texto(datos.recomendaciones(), "Las recomendaciones", 2000, true));
        if (datos.proximoSeguimiento() != null) contenido.put("proximoSeguimiento", datos.proximoSeguimiento().toString());
        return contenido;
    }

    static String texto(String valor, String campo, int maximo, boolean requerido) {
        String limpio = valor == null ? "" : valor.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (limpio.length() > maximo || limpio.indexOf(0) >= 0 || requerido && limpio.isEmpty()) {
            throw new IllegalArgumentException(campo + (requerido ? " es un campo obligatorio y admite hasta " : " admite hasta ")
                    + maximo + " caracteres.");
        }
        return limpio.isEmpty() ? null : limpio;
    }

    private Map<String, String> campos(LocalDate fecha, Etapa etapa, Map<String, String> contenido) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("fechaAtencion", fecha.toString());
        campos.put("etapa", etapa.name());
        campos.putAll(contenido);
        return campos;
    }

    static Map<String, Cambio> comparar(Map<String, String> anterior, Map<String, String> actual) {
        Set<String> campos = new LinkedHashSet<>(anterior.keySet());
        campos.addAll(actual.keySet());
        Map<String, Cambio> cambios = new LinkedHashMap<>();
        for (String campo : campos) {
            if (!Objects.equals(anterior.get(campo), actual.get(campo))) {
                cambios.put(campo, new Cambio(anterior.get(campo), actual.get(campo)));
            }
        }
        return cambios;
    }

    private Registro vista(SeguimientoVinculacion registro, Usuario usuario, boolean editable) {
        return new Registro(registro.getId(), registro.getRol(), registro.getNombreAutor(), registro.getRegistrado(),
                registro.getFechaAtencion(), registro.getEtapa(), contenido(registro), registro.getVersion(),
                editable && autor(registro, usuario));
    }

    private Map<String, String> contenido(SeguimientoVinculacion registro) {
        return leer(registro.getContenido(), new TypeReference<>() {});
    }

    private void guardarEvento(SeguimientoVinculacion registro, Usuario usuario, Instant fecha, Accion accion,
                                Map<String, Cambio> cambios, String motivo) {
        historial.save(new EventoSeguimientoVinculacion(registro, usuario, nombreCompleto(usuario), fecha, accion, escribir(cambios), motivo));
    }

    static String nombreCompleto(Usuario usuario) {
        String nombre = (Objects.toString(usuario.getNombre(), "") + " "
                + Objects.toString(usuario.getApellido(), "")).strip();
        return nombre.isEmpty() ? usuario.getIdentificador() : nombre;
    }

    String escribir(Object contenido) {
        try { return json.writeValueAsString(contenido); }
        catch (JsonProcessingException error) { throw new IllegalStateException("No se pudo conservar el seguimiento", error); }
    }

    <T> T leer(String contenido, TypeReference<T> tipo) {
        try { return json.readValue(contenido, tipo); }
        catch (JsonProcessingException error) { throw new IllegalStateException("No se pudo consultar el seguimiento", error); }
    }
}
