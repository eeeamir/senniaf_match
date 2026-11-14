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
    @PostMapping("/{id}/interrupcion")
    public Map<String,Object> registrarInterrupcion(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor,@jakarta.validation.Valid @RequestBody com.senniaf.match.dto.InterrupcionRequest solicitud){return service.registrarInterrupcion(id,rol,actor,solicitud);}
    @PostMapping("/{id}/interrupcion/revision")
    public Map<String,Object> revisarInterrupcion(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor,@jakarta.validation.Valid @RequestBody com.senniaf.match.dto.RevisionInterrupcionRequest solicitud){return service.revisarInterrupcion(id,rol,actor,solicitud);}
    @PostMapping("/{id}/cierre")
    public Map<String,Object> cerrar(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor,@jakarta.validation.Valid @RequestBody com.senniaf.match.dto.CierreInstitucionalRequest solicitud){return service.cerrar(id,rol,actor,solicitud);}
    public record Disponibilidad(@jakarta.validation.constraints.NotBlank(message="Selecciona el expediente revisado") String expediente,@jakarta.validation.constraints.Size(min=10,max=600,message="La revisión debe tener entre 10 y 600 caracteres") @jakarta.validation.constraints.NotBlank(message="Documenta la revisión individual") String justificacion){}
    @PostMapping("/{id}/disponibilidad")
    public Map<String,Object> disponibilidad(@PathVariable Long id,@RequestHeader("X-User-Role") String rol,@RequestHeader("X-User-Id") String actor,@jakarta.validation.Valid @RequestBody Disponibilidad solicitud){return service.revisarDisponibilidad(id,rol,actor,solicitud.expediente(),solicitud.justificacion());}
    private void familia(String rol){if(!"FAMILIA".equals(rol))throw new RolNoAutorizadoException("Solo la familia correspondiente puede responder esta propuesta");}
}
