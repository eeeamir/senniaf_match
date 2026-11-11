package com.senniaf.match.controller;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.PerfilNinoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Identidad y rol provienen de SesionFiltro; la responsabilidad se verifica en el servidor. */
@RestController
@RequestMapping("/api/ninos")
public class PerfilNinoController {

    private static final Set<String> ROLES_AUTORIZADOS = Set.of("TRABAJADOR_SOCIAL", "PSICOLOGO");

    private final PerfilNinoService service;
    private final com.senniaf.match.service.HistorialCasoService historial;

    public PerfilNinoController(PerfilNinoService service) {
        this(service,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public PerfilNinoController(PerfilNinoService service,com.senniaf.match.service.HistorialCasoService historial) {
        this.service=service;this.historial=historial;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPerfiles(
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-User-Id") String usuario,
            @RequestParam(defaultValue="false") boolean incluirInactivos) {

        if (!ROLES_AUTORIZADOS.contains(rol) && !"COMITE".equals(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para consultar perfiles de niños o niñas");
        }

        List<Map<String, Object>> perfiles = service.listarPerfiles(usuario, rol, incluirInactivos).stream()
                .map(perfil -> {
                    Map<String, Object> resumen = new LinkedHashMap<>();
                    resumen.put("codigoCaso", perfil.getCodigoCaso());
                    resumen.put("edad", perfil.getEdad());
                    resumen.put("unidadEdad", perfil.getUnidadEdad());
                    resumen.put("nivelEscolar", perfil.getNivelEscolar());
                    resumen.put("perfilCompleto", perfil.isPerfilCompleto());
                    resumen.put("alertaMedica", perfil.isAlertaMedica());
                    resumen.put("fechaRegistro", perfil.getFechaRegistro());
                    resumen.put("registradoPor", perfil.getRegistradoPor());
                    resumen.put("trabajadorSocialAsignado", perfil.getTrabajadorSocialAsignado());
                    resumen.put("psicologoAsignado", perfil.getPsicologoAsignado());
                    resumen.put("bajaLogica", perfil.isBajaLogica());
                    resumen.put("cierrePorAdopcion", perfil.isCierrePorAdopcion());
                    var pendientes=service.resumenSolicitudesPendientes(perfil);
                    resumen.put("solicitudesPendientes", pendientes.size());
                    resumen.put("solicitudesPendientesDetalle", pendientes);
                    return resumen;
                })
                .toList();

        return ResponseEntity.ok(perfiles);
    }

    @PostMapping
    public ResponseEntity<PerfilNinoResponse> registrarPerfil(
            @Valid @RequestBody PerfilNinoRequest request,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-User-Id") String usuarioId) {

        if (!"TRABAJADOR_SOCIAL".equals(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para registrar perfiles de niños o niñas");
        }

        PerfilNinoResponse response = service.registrarPerfil(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void comprobarProfesional(String rol) {
        if (!ROLES_AUTORIZADOS.contains(rol) && !"COMITE".equals(rol)) throw new RolNoAutorizadoException("Solo profesionales autorizados o Comité");
    }

    @GetMapping("/{codigo}")
    public com.senniaf.match.model.PerfilNino detalle(@PathVariable String codigo,
                      @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarProfesional(rol); return service.consultarExpediente(codigo, actor, rol);
    }
    @GetMapping("/{codigo}/auditoria")
    public java.util.List<?> auditoria(@PathVariable String codigo,
                      @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarProfesional(rol); var eventos=service.historial(codigo, actor, rol);
        return historial==null?eventos:historial.consultar(codigo,eventos);
    }
    @PutMapping("/{codigo}")
    public PerfilNinoResponse editar(@PathVariable String codigo, @Valid @RequestBody PerfilNinoRequest r,
                    @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarTrabajoSocial(rol); return service.actualizar(codigo, actor, r);
    }
    @DeleteMapping("/{codigo}")
    public Map<String, String> baja(@PathVariable String codigo,
                    @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        throw new RolNoAutorizadoException("La baja directa no está permitida. Solicita la desactivación para revisión del Comité.");
    }
    private void comprobarTrabajoSocial(String rol) {
        if (!"TRABAJADOR_SOCIAL".equals(rol))
            throw new RolNoAutorizadoException("Solo trabajo social puede registrar o modificar el perfil infantil");
    }

    @GetMapping("/{codigo}/evaluaciones-psicologicas")
    public List<com.senniaf.match.dto.EvaluacionPsicologicaResponse> evaluaciones(@PathVariable String codigo,
                    @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        return service.consultarEvaluaciones(codigo, actor, rol);
    }

    @PostMapping("/{codigo}/evaluaciones-psicologicas")
    public ResponseEntity<com.senniaf.match.dto.EvaluacionPsicologicaResponse> evaluar(@PathVariable String codigo,
                    @Valid @RequestBody com.senniaf.match.dto.EvaluacionPsicologicaRequest request,
                    @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEvaluacion(codigo, actor, rol, request));
    }

    @PutMapping("/{codigo}/evaluaciones-psicologicas/{id}")
    public com.senniaf.match.dto.EvaluacionPsicologicaResponse corregirEvaluacion(@PathVariable String codigo, @PathVariable Long id,
            @Valid @RequestBody com.senniaf.match.dto.EvaluacionPsicologicaRequest request,
            @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        return service.corregirEvaluacion(codigo,id,actor,rol,request);
    }

    @GetMapping("/solicitudes-estado/pendientes")
    public List<com.senniaf.match.dto.SolicitudEstadoResponse> pendientes(@RequestHeader("X-User-Role") String rol) {
        return service.solicitudesPendientes(rol);
    }

    @GetMapping("/{codigo}/solicitudes-estado")
    public List<com.senniaf.match.dto.SolicitudEstadoResponse> solicitudes(@PathVariable String codigo,
            @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        return service.consultarSolicitudes(codigo, actor, rol);
    }

    @PostMapping("/{codigo}/solicitudes-estado")
    public ResponseEntity<com.senniaf.match.dto.SolicitudEstadoResponse> solicitar(@PathVariable String codigo,
            @Valid @RequestBody com.senniaf.match.dto.SolicitudEstadoRequest request,
            @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarCambioEstado(codigo, actor, rol, request));
    }

    @PostMapping("/solicitudes-estado/{id}/resolucion")
    public com.senniaf.match.dto.SolicitudEstadoResponse resolver(@PathVariable Long id,
            @Valid @RequestBody com.senniaf.match.dto.ResolverSolicitudRequest request,
            @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        return service.resolverSolicitud(id, actor, rol, request);
    }
}
