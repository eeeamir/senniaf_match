package com.senniaf.match.controller;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.PerfilFamiliaService;
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
 * Módulo de perfil de idoneidad de la familia adoptante (PBI06/PBI07,
 * HU02). Solo el rol FAMILIA puede completar su propia entrevista; ni el
 * trabajador social, ni el psicólogo, ni el Comité crean este perfil por
 * la familia en este endpoint. Es, a propósito, la contraparte exacta de
 * PerfilNinoController: cada rol tiene un único endpoint de registro que
 * le corresponde y no puede usar el del otro.
 *
 * NOTA: mismo mecanismo simple de rol por cabecera (X-User-Role) usado en
 * PerfilNinoController; ver esa clase para el comentario sobre
 * reemplazarlo por autenticación real (Spring Security/JWT).
 */
@RestController
@RequestMapping("/api/familias")
public class PerfilFamiliaController {

    private static final Set<String> ROLES_AUTORIZADOS = Set.of("FAMILIA");

    private final PerfilFamiliaService service;

    public PerfilFamiliaController(PerfilFamiliaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PerfilFamiliaResponse> registrarPerfil(
            @Valid @RequestBody PerfilFamiliaRequest request,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-User-Id") String usuarioId) {

        if (!ROLES_AUTORIZADOS.contains(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para registrar perfiles de familia");
        }

        PerfilFamiliaResponse response = service.registrarPerfil(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
