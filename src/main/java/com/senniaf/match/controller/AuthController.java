package com.senniaf.match.controller;

import com.senniaf.match.dto.*;
import com.senniaf.match.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService service;
    private final com.senniaf.match.service.AutenticacionService autenticacion;
    public AuthController(UsuarioService service, com.senniaf.match.service.AutenticacionService autenticacion){this.service=service;this.autenticacion=autenticacion;}
    @PostMapping("/registro") public ResponseEntity<Map<String,Object>> registro(@Valid @RequestBody RegistroUsuarioRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(r));}
    @PostMapping("/verificar") public ResponseEntity<Map<String,Object>> verificar(@Valid @RequestBody VerificarCodigoRequest r){return ResponseEntity.ok(service.verificar(r));}
    @PostMapping("/reenviar") public ResponseEntity<Map<String,Object>> reenviar(@RequestBody Map<String,String> r){return ResponseEntity.ok(service.reenviar(r.getOrDefault("cedula","")));}
    @PostMapping("/salir") public ResponseEntity<Void> salir(@RequestHeader(value="Authorization",required=false) String header){autenticacion.cerrar(header);return ResponseEntity.noContent().build();}
    @PostMapping("/login") public ResponseEntity<Map<String,Object>> login(@Valid @RequestBody LoginRequest r){return ResponseEntity.ok(service.login(r));}
}
