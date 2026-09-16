package com.senniaf.match.controller;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.PerfilNinoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

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

    @PostMapping
    public ResponseEntity<PerfilNinoResponse> registrarPerfil(
            @Valid @RequestBody PerfilNinoRequest request,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "desconocido") String usuarioId) {

        if (!ROLES_AUTORIZADOS.contains(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para registrar perfiles de niños o niñas");
        }

        PerfilNinoResponse response = service.registrarPerfil(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
