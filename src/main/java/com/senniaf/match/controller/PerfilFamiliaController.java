package com.senniaf.match.controller;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.PerfilFamiliaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Registra, consulta y actualiza únicamente la entrevista de la familia autenticada.
 * SesionFiltro obtiene identidad y rol de la sesión y sustituye las cabeceras del cliente.
 * El servicio conserva el mismo expediente y cuenta una reevaluación solo al completarla.
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

    /**
     * Permite a la familia saber si ya completó su entrevista (200) o no
     * (404). El inicio de sesión usa esto para decidir si la lleva al
     * formulario o a la pantalla del motor de match.
     */
    @GetMapping("/mi-perfil")
    public ResponseEntity<PerfilFamiliaResponse> consultarMiPerfil(
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-User-Id") String usuarioId) {

        if (!ROLES_AUTORIZADOS.contains(rol)) {
            throw new RolNoAutorizadoException(
                    "El rol '" + rol + "' no está autorizado para consultar perfiles de familia");
        }

        return service.consultarPorCedula(usuarioId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/mi-perfil/detalle")
    public ResponseEntity<?> detalle(@RequestHeader("X-User-Role") String rol,
                                     @RequestHeader("X-User-Id") String id) {
        if (!"FAMILIA".equals(rol)) throw new RolNoAutorizadoException("Solo la familia puede consultar su entrevista");
        return service.consultarDetallePropio(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/mi-perfil")
    public PerfilFamiliaResponse corregir(@Valid @RequestBody PerfilFamiliaRequest request,
                                           @RequestHeader("X-User-Role") String rol,
                                           @RequestHeader("X-User-Id") String id) {
        if (!"FAMILIA".equals(rol)) throw new RolNoAutorizadoException("Solo la familia puede corregir su entrevista");
        return service.actualizarPropio(request, id);
    }
}
