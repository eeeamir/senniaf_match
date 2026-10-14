package com.senniaf.match.service;

import com.senniaf.match.dto.*;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.security.SecureRandom;

@Service
public class UsuarioService {
    private static final int MAX_INTENTOS = 5;
    private final SecureRandom random = new SecureRandom();
    @org.springframework.beans.factory.annotation.Value("${senniaf.demo:false}") private boolean demo;
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AutenticacionService autenticacion;
    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, AutenticacionService autenticacion){this.repo=repo;this.encoder=encoder;this.autenticacion=autenticacion;}

    public Map<String,Object> registrar(RegistroUsuarioRequest r){
        if(!demo) throw new IllegalStateException("Registro temporal deshabilitado: falta integrar un proveedor de correo real.");
        String cedula=r.getCedula().trim().toUpperCase(), correo=r.getCorreo().trim().toLowerCase();
        if(repo.existsByCedula(cedula)) throw new IllegalStateException("Esta cédula ya está registrada. Inicia sesión para continuar.");
        if(repo.existsByCorreoIgnoreCase(correo)) throw new IllegalStateException("Este correo ya está registrado. Inicia sesión para continuar.");
        String codigo=nuevoCodigo();
        Usuario u=new Usuario(); u.setIdentificador(cedula); u.setNombre(r.getNombre().trim()); u.setApellido(r.getApellido().trim());
        u.setCedula(cedula); u.setCorreo(correo); u.setPasswordHash(encoder.encode(r.getPassword())); u.setRol("FAMILIA");
        u.setVerificado(false); u.setCodigoVerificacionHash(encoder.encode(codigo)); u.setCodigoExpira(LocalDateTime.now().plusMinutes(10));
        u.setIntentosVerificacion(0); u.setFechaCreacion(LocalDateTime.now()); repo.save(u);
        return Map.of("mensaje","Registro creado. Código de prueba generado.","codigoDemo",codigo,"correo",correo);
    }
    public Map<String,Object> reenviar(String cedula){
        if(!demo) throw new IllegalStateException("El reenvío requiere integrar un proveedor de correo.");
        Usuario u=usuarioFamilia(cedula); String codigo=nuevoCodigo();
        u.setCodigoVerificacionHash(encoder.encode(codigo)); u.setCodigoExpira(LocalDateTime.now().plusMinutes(10)); u.setIntentosVerificacion(0); repo.save(u);
        return Map.of("mensaje","Se generó un código de prueba.","codigoDemo",codigo);
    }
    public Map<String,Object> verificar(VerificarCodigoRequest r){
        Usuario u=usuarioFamilia(r.getCedula());
        if(u.isVerificado()) return sesion(u);
        if(u.getCodigoExpira()==null || LocalDateTime.now().isAfter(u.getCodigoExpira())) throw new IllegalArgumentException("El código venció. Pide uno nuevo.");
        if(u.getIntentosVerificacion()>=MAX_INTENTOS) throw new IllegalArgumentException("Demasiados intentos. Pide un código nuevo.");
        if(!encoder.matches(r.getCodigo(),u.getCodigoVerificacionHash())) {u.setIntentosVerificacion(u.getIntentosVerificacion()+1);repo.save(u);throw new IllegalArgumentException("Código incorrecto. Te quedan "+(MAX_INTENTOS-u.getIntentosVerificacion())+" intentos.");}
        u.setVerificado(true); u.setCodigoVerificacionHash(null); u.setCodigoExpira(null); u.setIntentosVerificacion(0); repo.save(u); return sesion(u);
    }
    public Map<String,Object> login(LoginRequest r){
        Usuario u=repo.findByIdentificador(r.getIdentificador().trim().toUpperCase()).orElse(null);
        if(u==null) u=repo.findByCorreoIgnoreCase(r.getIdentificador().trim()).orElse(null);
        if(u==null || !encoder.matches(r.getPassword(),u.getPasswordHash())) throw new IllegalArgumentException("Cédula, correo, ID o contraseña incorrectos.");
        if(!u.isVerificado()) throw new IllegalArgumentException("Debes verificar tu correo antes de iniciar sesión.");
        return sesion(u);
    }
    private Usuario usuarioFamilia(String cedula){return repo.findByCedula(cedula.trim().toUpperCase()).orElseThrow(()->new IllegalArgumentException("No encontramos esa cédula. Regístrate primero."));}
    private Map<String,Object> sesion(Usuario u){
        String nombre=(u.getNombre()==null?u.getIdentificador():u.getNombre()+" "+(u.getApellido()==null?"":u.getApellido())).trim();
        return Map.of("tipo",u.getRol().equals("FAMILIA")?"FAMILIA":"FUNCIONARIO","id",u.getIdentificador(),"nombre",nombre,"rol",u.getRol(),"token",autenticacion.iniciar(u));
    }
    private String nuevoCodigo(){return String.format("%06d",random.nextInt(1_000_000));}
}
