package com.senniaf.match.controller;
import com.senniaf.match.dto.*;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/familias")
public class PerfilFamiliaController {
    private final PerfilFamiliaService service;
    private final AutenticacionService auth;
    public PerfilFamiliaController(PerfilFamiliaService service,AutenticacionService auth){this.service=service;this.auth=auth;}
    @PostMapping public ResponseEntity<PerfilFamiliaResponse> registrar(@Valid @RequestBody PerfilFamiliaRequest r,@RequestHeader(value="Authorization",required=false) String header){
        Usuario u=auth.requerir(header,"FAMILIA");return ResponseEntity.status(201).body(service.registrarPerfil(r,u.getIdentificador()));
    }
    @GetMapping("/mi-perfil") public ResponseEntity<PerfilFamiliaResponse> miPerfil(@RequestHeader(value="Authorization",required=false) String header){
        Usuario u=auth.requerir(header,"FAMILIA");
        java.util.Optional<PerfilFamiliaResponse> perfil=service.consultarPorCedula(u.getIdentificador());
        if(perfil.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(perfil.get());
    }
}
