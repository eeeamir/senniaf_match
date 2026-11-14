package com.senniaf.match.controller;

import com.senniaf.match.service.VinculacionService;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/vinculaciones")
public class VinculacionController {
    private final VinculacionService service;
    public VinculacionController(VinculacionService service){this.service=service;}
    @GetMapping public List<Map<String,Object>> listar(@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor,@RequestParam(required=false) String estado,@RequestParam(required=false) String buscar){return service.listar(rol,actor,estado,buscar);}
    @GetMapping("/propias") public List<Map<String,Object>> propias(@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){familia(rol);return service.propias(actor);}
    @GetMapping("/{id}") public Map<String,Object> detalle(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor){return service.detalle(id,rol,actor);}
    public record Respuesta(String respuesta){}
    @PostMapping("/{id}/respuesta") public Map<String,Object> responder(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor,@RequestBody Respuesta request){familia(rol);return service.responder(id,actor,request==null?null:request.respuesta());}
    private void familia(String rol){if(!"FAMILIA".equals(rol))throw new RolNoAutorizadoException("Solo la familia correspondiente puede responder esta propuesta");}
}
