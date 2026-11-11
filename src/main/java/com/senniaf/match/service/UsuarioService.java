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
            throw new com.senniaf.match.web.CampoInvalidoException("password","La contraseña no puede exceder 72 bytes UTF-8",false);
    }

    @Transactional
    public Map<String,Object> registrar(RegistroUsuarioRequest r){
        registroDisponible(); r.validar(); passwordValida(r.getPassword());
        var tipo=r.tipoEfectivo(); String numero=r.numeroNormalizado();
        String identificador=tipo.identificador(numero), correo=r.getCorreo().strip().toLowerCase(Locale.ROOT);
        if(repo.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(tipo,numero) || repo.existsByCedula(numero)
                || repo.findByIdentificador(identificador).isPresent() || repo.findByIdentificador(numero).isPresent())
            throw new com.senniaf.match.web.CampoInvalidoException("numeroDocumento","Este documento ya está registrado. Inicia sesión con tu documento o correo.",true);
        if(repo.existsByCorreoIgnoreCase(correo))
            throw new com.senniaf.match.web.CampoInvalidoException("correo","Este correo ya está registrado. Inicia sesión para continuar.",true);
        String codigo=nuevoCodigo();
        Usuario u=new Usuario(); u.setIdentificador(identificador); u.setNombre(r.getNombre()); u.setApellido(r.getApellido());
        u.setTipoDocumento(tipo);u.setNumeroDocumento(numero);
        u.setCedula(tipo==com.senniaf.match.model.enums.TipoDocumento.PASAPORTE?null:numero);
        u.setCorreo(correo); u.setPasswordHash(encoder.encode(r.getPassword())); u.setRol("FAMILIA");
        u.setVerificado(false); u.setCodigoVerificacionHash(encoder.encode(codigo)); u.setCodigoExpira(LocalDateTime.now().plusMinutes(10));
        u.setIntentosVerificacion(0); u.setFechaCreacion(LocalDateTime.now());
        try { repo.saveAndFlush(u); }
        catch(org.springframework.dao.DataIntegrityViolationException conflicto) {
            String detalle=conflicto.getMostSpecificCause().getMessage();
            boolean esCorreo=detalle!=null && detalle.contains("uk_usuario_correo");
            throw new com.senniaf.match.web.CampoInvalidoException(esCorreo?"correo":"numeroDocumento",
                    esCorreo?"Este correo ya está registrado. Inicia sesión para continuar.":"Este documento ya está registrado. Inicia sesión para continuar.",true);
        }
        Map<String,Object> respuesta=entregarCodigo(u,codigo);
        respuesta.put("correo",u.getCorreo());respuesta.put("identificador",u.getIdentificador());return respuesta;
    }
    @Transactional
    public Map<String,Object> reenviar(String cedula){
        registroDisponible();
        if(cedula==null || cedula.isBlank() || cedula.length()>40) throw new IllegalArgumentException("Indica una referencia de cuenta válida");
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
        if(u==null && !r.getIdentificador().contains("@")) u=repo.findByIdentificador("PAS-"+r.getIdentificador().strip().toUpperCase(Locale.ROOT)).orElse(null);
        if(u==null) u=repo.findByCorreoIgnoreCase(r.getIdentificador().strip()).orElse(null);
        boolean coincide=encoder.matches(r.getPassword(),u==null?hashInexistente:u.getPasswordHash());
        if(u==null || !coincide) throw new IllegalArgumentException("Documento, correo, ID o contraseña incorrectos.");
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
    private Usuario usuarioFamilia(String referencia){
        String id=referencia.strip().toUpperCase(Locale.ROOT);
        var u=repo.bloquearPorIdentificador(id);
        if(u.isEmpty()&&!id.startsWith("PAS-"))u=repo.bloquearPorIdentificador("PAS-"+id);
        if(u.isEmpty())u=repo.bloquearPorCedula(id);
        return u.orElseThrow(()->new IllegalArgumentException("No encontramos esa cuenta. Revisa el documento o regístrate primero."));
    }
    private Map<String,Object> sesion(Usuario u){
        String nombre=(u.getNombre()==null?u.getIdentificador():u.getNombre()+" "+(u.getApellido()==null?"":u.getApellido())).trim();
        return Map.of("tipo",u.getRol().equals("FAMILIA")?"FAMILIA":"FUNCIONARIO","id",u.getIdentificador(),"nombre",nombre,"rol",u.getRol(),"token",sesiones.crear(u));
    }
    private String nuevoCodigo(){return String.format("%06d",ALEATORIO.nextInt(1_000_000));}
}
