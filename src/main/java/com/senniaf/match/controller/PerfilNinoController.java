package com.senniaf.match.controller;
import com.senniaf.match.dto.*;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/ninos")
public class PerfilNinoController {
    private final PerfilNinoService service;
    private final AutenticacionService auth;
    private final AuditoriaService auditoria;
    public PerfilNinoController(PerfilNinoService service,AutenticacionService auth,AuditoriaService auditoria){this.service=service;this.auth=auth;this.auditoria=auditoria;}
    private Usuario profesional(String header){return auth.requerir(header,"TRABAJADOR_SOCIAL","PSICOLOGO");}
    @GetMapping public List<Map<String,Object>> listar(@RequestHeader(value="Authorization",required=false) String header){
        Usuario u=profesional(header);
        return service.listarPerfiles(u.getIdentificador()).stream().map(p->{
          Map<String,Object> m=new LinkedHashMap<>();m.put("codigoCaso",p.getCodigoCaso());m.put("edad",p.getEdad());
          m.put("nivelEscolar",p.getNivelEscolar());m.put("perfilCompleto",p.isPerfilCompleto());m.put("alertaMedica",p.isAlertaMedica());
          m.put("fechaRegistro",p.getFechaRegistro());m.put("registradoPor",p.getRegistradoPor());return m;}).toList();
    }
    @GetMapping("/{codigo}") public PerfilNino consultar(@PathVariable String codigo,@RequestHeader(value="Authorization",required=false) String header){return service.consultar(codigo,profesional(header).getIdentificador());}
    @GetMapping("/{codigo}/auditoria") public Object auditoria(@PathVariable String codigo,@RequestHeader(value="Authorization",required=false) String header){
        Usuario u=profesional(header);service.consultar(codigo,u.getIdentificador());return auditoria.porCaso(codigo);
    }
    @PostMapping public ResponseEntity<PerfilNinoResponse> registrar(@Valid @RequestBody PerfilNinoRequest request,@RequestHeader(value="Authorization",required=false) String header){
        Usuario u=profesional(header);return ResponseEntity.status(201).body(service.registrarPerfil(request,u.getIdentificador()));
    }
    @PutMapping("/{codigo}") public PerfilNinoResponse editar(@PathVariable String codigo,@Valid @RequestBody PerfilNinoRequest r,@RequestHeader(value="Authorization",required=false) String header){return service.editar(codigo,r,profesional(header).getIdentificador());}
    @DeleteMapping("/{codigo}") public ResponseEntity<Void> eliminar(@PathVariable String codigo,@RequestHeader(value="Authorization",required=false) String header){service.eliminar(codigo,profesional(header).getIdentificador());return ResponseEntity.noContent().build();}
}
