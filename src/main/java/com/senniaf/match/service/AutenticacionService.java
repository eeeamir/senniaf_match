package com.senniaf.match.service;
import com.senniaf.match.model.*;
import com.senniaf.match.repository.SesionAccesoRepository;
import com.senniaf.match.security.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class AutenticacionService {
    private final SesionAccesoRepository sesiones;
    private final SecureRandom random = new SecureRandom();
    public AutenticacionService(SesionAccesoRepository sesiones){this.sesiones=sesiones;}
    public String iniciar(Usuario usuario){
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SesionAcceso s=new SesionAcceso(); s.setTokenHash(hash(token)); s.setUsuario(usuario);
        s.setExpira(LocalDateTime.now().plusHours(8)); sesiones.save(s); return token;
    }
    public Usuario requerir(String header, String... roles){
        if(header==null || !header.startsWith("Bearer ")) throw new AccesoNoAutorizadoException("Inicia sesión para continuar.");
        String token=header.substring(7).trim();
        if(!token.matches("[A-Za-z0-9_-]{43}")) throw new AccesoNoAutorizadoException("Sesión inválida.");
        SesionAcceso sesion=sesiones.findById(hash(token)).orElseThrow(()->new AccesoNoAutorizadoException("Sesión vencida o no válida."));
        if(sesion.getExpira().isBefore(LocalDateTime.now())) { sesiones.delete(sesion); throw new AccesoNoAutorizadoException("Tu sesión ha vencido."); }
        Usuario usuario=sesion.getUsuario();
        if(!usuario.isVerificado()) throw new AccesoNoAutorizadoException("Usuario no verificado.");
        if(Arrays.stream(roles).noneMatch(usuario.getRol()::equals)) throw new RolNoAutorizadoException("Tu rol no está autorizado para esta operación.");
        return usuario;
    }
    public void cerrar(String header){ if(header==null || !header.startsWith("Bearer ")) return;
        String token=header.substring(7).trim(); if(token.matches("[A-Za-z0-9_-]{43}")) sesiones.deleteById(hash(token)); }
    private String hash(String token){try{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
