package com.senniaf.match.controller;

import com.senniaf.match.service.CitaService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private final CitaService servicio;
    public CitaController(CitaService servicio){this.servicio=servicio;}
    @GetMapping public List<Map<String,Object>> listar(@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return servicio.listar(rol,actor);}
    @GetMapping("/opciones") public Map<String,Object> opciones(@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return servicio.opciones(rol,actor);}
    @GetMapping("/disponibilidad") public List<String> disponibilidad(@RequestParam Long vinculacionId,@RequestParam String fecha,@RequestParam(defaultValue="120") int duracion,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return servicio.disponibilidad(vinculacionId,fecha,duracion,rol,actor);}
    @GetMapping("/{id}") public Map<String,Object> detalle(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return servicio.detalle(id,rol,actor);}
    @PostMapping public Map<String,Object> solicitar(@RequestBody CitaService.Solicitud r,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return servicio.solicitar(r,rol,actor);}
    @PostMapping("/{id}/acciones") public Map<String,Object> responder(@PathVariable Long id,@RequestBody CitaService.Respuesta r,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return servicio.responder(id,r,rol,actor);}
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,org.springframework.dao.PessimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class,jakarta.persistence.PessimisticLockException.class})
    public org.springframework.http.ResponseEntity<Map<String,String>> conflictoConcurrente(RuntimeException error){
        return org.springframework.http.ResponseEntity.status(409).body(Map.of("mensaje","La solicitud cambió o está siendo atendida. Actualiza la información antes de responder"));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public org.springframework.http.ResponseEntity<Map<String,String>> parametrosInvalidos(Exception error){
        return org.springframework.http.ResponseEntity.badRequest().body(Map.of("mensaje","Selecciona una vinculación, fecha y duración válidas"));
    }
}
