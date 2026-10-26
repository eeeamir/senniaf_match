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

/**
 * Módulo de perfil de niño/a (sección 2.12). Solo un trabajador social o
 * psicólogo puede crear el perfil; la familia nunca accede a este endpoint.
 *
 * NOTA: el rol se lee aquí de una cabecera HTTP simple (X-User-Role) solo
 * como continuación directa de la prueba de autorización por rol hecha en
 * el Sprint 1 con Burp Suite. Si el proyecto ya tiene (o va a tener) un
 * mecanismo real de autenticación (Spring Security, JWT, etc.), esta
 * verificación debe reemplazarse por ese mecanismo, por ejemplo con
 * @PreAuthorize("hasRole('TRABAJADOR_SOCIAL')").
 */
@RestController
@RequestMapping("/api/ninos")
public class PerfilNinoController {

    private static final Set<String> ROLES_AUTORIZADOS = Set.of("TRABAJADOR_SOCIAL", "PSICOLOGO");

    private final PerfilNinoService service;

    public PerfilNinoController(PerfilNinoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPerfiles(
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-User-Id") String usuario) {

        if (!ROLES_AUTORIZADOS.contains(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para consultar perfiles de niños o niñas");
        }

        List<Map<String, Object>> perfiles = service.listarPerfiles(usuario).stream()
                .map(perfil -> {
                    Map<String, Object> resumen = new LinkedHashMap<>();
                    resumen.put("codigoCaso", perfil.getCodigoCaso());
                    resumen.put("edad", perfil.getEdad());
                    resumen.put("nivelEscolar", perfil.getNivelEscolar());
                    resumen.put("perfilCompleto", perfil.isPerfilCompleto());
                    resumen.put("alertaMedica", perfil.isAlertaMedica());
                    resumen.put("fechaRegistro", perfil.getFechaRegistro());
                    resumen.put("registradoPor", perfil.getRegistradoPor());
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

        if (!ROLES_AUTORIZADOS.contains(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para registrar perfiles de niños o niñas");
        }

        PerfilNinoResponse response = service.registrarPerfil(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void comprobarProfesional(String rol) {
        if (!ROLES_AUTORIZADOS.contains(rol)) throw new RolNoAutorizadoException("Solo profesionales autorizados");
    }

    @GetMapping("/{codigo}")
    public com.senniaf.match.model.PerfilNino detalle(@PathVariable String codigo,
                      @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarProfesional(rol); return service.propio(codigo, actor);
    }
    @GetMapping("/{codigo}/auditoria")
    public java.util.List<com.senniaf.match.model.EventoCaso> auditoria(@PathVariable String codigo,
                      @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarProfesional(rol); return service.historial(codigo, actor);
    }
    @PutMapping("/{codigo}")
    public PerfilNinoResponse editar(@PathVariable String codigo, @Valid @RequestBody PerfilNinoRequest r,
                    @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarProfesional(rol); return service.actualizar(codigo, actor, r);
    }
    @DeleteMapping("/{codigo}")
    public Map<String, String> baja(@PathVariable String codigo,
                    @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        comprobarProfesional(rol); service.darDeBaja(codigo, actor);
        return Map.of("mensaje", "Caso dado de baja; se conserva auditoría");
    }
}
