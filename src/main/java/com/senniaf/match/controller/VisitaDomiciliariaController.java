package com.senniaf.match.controller;

import com.senniaf.match.model.Usuario;
import com.senniaf.match.security.SesionFiltro;
import com.senniaf.match.service.VisitaDomiciliariaService;
import com.senniaf.match.service.VisitaDomiciliariaService.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vinculaciones/{id}/visitas-domiciliarias")
public class VisitaDomiciliariaController {
    private final VisitaDomiciliariaService servicio;

    public VisitaDomiciliariaController(VisitaDomiciliariaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<Consulta> consultar(@PathVariable Long id,
            @RequestAttribute(name = SesionFiltro.USUARIO_AUTENTICADO, required = false) Usuario usuario) {
        return confidencial(servicio.consultar(id, usuario));
    }

    @PostMapping
    public ResponseEntity<Registro> registrar(@PathVariable Long id,
            @RequestAttribute(name = SesionFiltro.USUARIO_AUTENTICADO, required = false) Usuario usuario,
            @RequestBody Solicitud datos) {
        return confidencial(servicio.registrar(id, usuario, datos));
    }

    @PutMapping("/{visitaId}")
    public ResponseEntity<Registro> editar(@PathVariable Long id, @PathVariable Long visitaId,
            @RequestAttribute(name = SesionFiltro.USUARIO_AUTENTICADO, required = false) Usuario usuario,
            @RequestBody Solicitud datos) {
        return confidencial(servicio.editar(id, visitaId, usuario, datos));
    }

    private <T> ResponseEntity<T> confidencial(T datos) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datos);
    }
}
