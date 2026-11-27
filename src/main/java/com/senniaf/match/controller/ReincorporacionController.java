package com.senniaf.match.controller;

import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.ReincorporacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reincorporaciones")
public class ReincorporacionController {
    private final ReincorporacionService servicio;
    public ReincorporacionController(ReincorporacionService servicio) { this.servicio = servicio; }

    public record Solicitud(@AssertTrue boolean confirmacion) {}
    public record Decision(@NotNull Boolean autorizar, @NotBlank @Size(min = 10, max = 2000) String fundamento,
                           @AssertTrue boolean confirmacion) {}

    @PostMapping("/vinculaciones/{id}")
    public Map<String, String> solicitar(@PathVariable Long id, @Valid @RequestBody Solicitud datos,
                          @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        exigirRol(rol, "FAMILIA");
        servicio.solicitar(id, actor, datos.confirmacion());
        return Map.of("mensaje", "Solicitud registrada");
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestHeader("X-User-Role") String rol) {
        exigirRol(rol, "COMITE");
        return servicio.listar();
    }

    @PostMapping("/{id}/resolucion")
    public Map<String, String> resolver(@PathVariable Long id, @Valid @RequestBody Decision datos,
                         @RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor) {
        exigirRol(rol, "COMITE");
        servicio.resolver(id, actor, datos.autorizar(), datos.fundamento(), datos.confirmacion());
        return Map.of("mensaje", "Resolución registrada");
    }

    private void exigirRol(String rol, String autorizado) {
        if (!autorizado.equals(rol)) throw new RolNoAutorizadoException("No tienes permiso para esta actuación");
    }
}
