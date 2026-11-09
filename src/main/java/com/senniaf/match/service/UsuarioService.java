package com.senniaf.match.service;

import com.senniaf.match.dto.*;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private static final int MAX_INTENTOS = 5;
    private static final SecureRandom ALEATORIO=new SecureRandom();
    @Value("${senniaf.auth.codigo-demo:false}") private boolean codigoDemo;
    private final com.senniaf.match.security.LimiteIntentos limites;
    private final String hashInexistente;
    private final CorreoVerificacionService correo;
    @Value("${senniaf.auth.registro-publico:false}") private boolean registroPublico;
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final SesionAccesoService sesiones;
    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, SesionAccesoService sesiones, com.senniaf.match.security.LimiteIntentos limites, CorreoVerificacionService correo){
        this.repo=repo;this.encoder=encoder;this.sesiones=sesiones;this.limites=limites;this.correo=correo;
        this.hashInexistente=encoder.encode(java.util.UUID.randomUUID().toString());
    }
    private void registroDisponible() {
        if(!codigoDemo && !(registroPublico && correo.disponible())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "El registro público no está habilitado en esta demostración. Usa las cuentas de prueba proporcionadas");
    }
    private void passwordValida(String password) {
        if(password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("La contraseña no puede exceder 72 bytes UTF-8");
    }

    @Transactional
    public Map<String,Object> registrar(RegistroUsuarioRequest r){
        registroDisponible(); passwordValida(r.getPassword());
        String cedula=r.getCedula().trim().toUpperCase(Locale.ROOT), correo=r.getCorreo().trim().toLowerCase(Locale.ROOT);
        if(repo.existsByCedula(cedula)) throw new IllegalStateException("Esta cédula ya está registrada. Inicia sesión para continuar.");
        if(repo.existsByCorreoIgnoreCase(correo)) throw new IllegalStateException("Este correo ya está registrado. Inicia sesión para continuar.");
        String codigo=nuevoCodigo();
        Usuario u=new Usuario(); u.setIdentificador(cedula); u.setNombre(r.getNombre().trim()); u.setApellido(r.getApellido().trim());
        u.setCedula(cedula); u.setCorreo(correo); u.setPasswordHash(encoder.encode(r.getPassword())); u.setRol("FAMILIA");
        u.setVerificado(false); u.setCodigoVerificacionHash(encoder.encode(codigo)); u.setCodigoExpira(LocalDateTime.now().plusMinutes(10));
        u.setIntentosVerificacion(0); u.setFechaCreacion(LocalDateTime.now()); repo.save(u);
        Map<String,Object> respuesta=entregarCodigo(u,codigo);
        respuesta.put("correo",u.getCorreo());return respuesta;
    }
    @Transactional
    public Map<String,Object> reenviar(String cedula){
        registroDisponible();
        if(cedula==null || cedula.isBlank() || cedula.length()>30) throw new IllegalArgumentException("Indica una referencia de cuenta válida");
        Usuario u=usuarioFamilia(cedula);
        if(u.isVerificado()) throw new IllegalStateException("La cuenta ya está verificada. Inicia sesión para continuar");
        if(!limites.admitir("codigo:"+u.getId(),3,900000)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Espera antes de solicitar otro código");
        String codigo;
        do {codigo=nuevoCodigo();} while(u.getCodigoVerificacionHash()!=null && encoder.matches(codigo,u.getCodigoVerificacionHash()));
        Map<String,Object> respuesta=entregarCodigo(u,codigo);
        u.setCodigoVerificacionHash(encoder.encode(codigo)); u.setCodigoExpira(LocalDateTime.now().plusMinutes(10)); u.setIntentosVerificacion(0); repo.save(u);
        return respuesta;
    }
    @Transactional(noRollbackFor=IllegalArgumentException.class)
    public Map<String,Object> verificar(VerificarCodigoRequest r){
        registroDisponible();
        Usuario u=usuarioFamilia(r.getCedula());
        if(u.isVerificado()) throw new IllegalStateException("La cuenta ya está verificada. Inicia sesión para continuar.");
        if(u.getCodigoExpira()==null || LocalDateTime.now().isAfter(u.getCodigoExpira())) throw new IllegalArgumentException("El código venció. Pide uno nuevo.");
        if(u.getIntentosVerificacion()>=MAX_INTENTOS) throw new IllegalArgumentException("Demasiados intentos. Pide un código nuevo.");
        if(!encoder.matches(r.getCodigo(),u.getCodigoVerificacionHash())) {u.setIntentosVerificacion(u.getIntentosVerificacion()+1);repo.save(u);throw new IllegalArgumentException("Código incorrecto. Te quedan "+(MAX_INTENTOS-u.getIntentosVerificacion())+" intentos.");}
        u.setVerificado(true); u.setCodigoVerificacionHash(null); u.setCodigoExpira(null); u.setIntentosVerificacion(0); repo.save(u); return sesion(u);
    }
    public Map<String,Object> login(LoginRequest r){
        passwordValida(r.getPassword());
        String clave="login:"+r.getIdentificador().strip().toUpperCase(Locale.ROOT);
        if(!limites.admitir(clave,10,900000)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos. Inténtalo más tarde");
        Usuario u=repo.findByIdentificador(r.getIdentificador().trim().toUpperCase(Locale.ROOT)).orElse(null);
        if(u==null) u=repo.findByCorreoIgnoreCase(r.getIdentificador().trim()).orElse(null);
        boolean coincide=encoder.matches(r.getPassword(),u==null?hashInexistente:u.getPasswordHash());
        if(u==null || !coincide) throw new IllegalArgumentException("Cédula, correo, ID o contraseña incorrectos.");
        if(!u.isVerificado()) throw new IllegalArgumentException("Debes verificar tu correo antes de iniciar sesión.");
        limites.limpiar(clave);return sesion(u);
    }
    private Map<String,Object> entregarCodigo(Usuario u,String codigo) {
        Map<String,Object> respuesta=new java.util.LinkedHashMap<>();
        if(correo.habilitado()) {
            correo.enviar(u.getCorreo(),codigo);
            respuesta.put("mensaje","Se envió un código a tu correo. Vence en 10 minutos; revisa también la carpeta de correo no deseado");
        } else if(codigoDemo) {
            respuesta.put("mensaje","Código generado únicamente para pruebas locales ficticias");respuesta.put("codigoDemo",codigo);
        } else throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"El correo no está disponible");
        return respuesta;
    }
    private Usuario usuarioFamilia(String cedula){return repo.bloquearPorCedula(cedula.trim().toUpperCase(Locale.ROOT)).orElseThrow(()->new IllegalArgumentException("No encontramos esa cédula. Regístrate primero."));}
    private Map<String,Object> sesion(Usuario u){
        String nombre=(u.getNombre()==null?u.getIdentificador():u.getNombre()+" "+(u.getApellido()==null?"":u.getApellido())).trim();
        return Map.of("tipo",u.getRol().equals("FAMILIA")?"FAMILIA":"FUNCIONARIO","id",u.getIdentificador(),"nombre",nombre,"rol",u.getRol(),"token",sesiones.crear(u));
    }
    private String nuevoCodigo(){return String.format("%06d",ALEATORIO.nextInt(1_000_000));}
}
