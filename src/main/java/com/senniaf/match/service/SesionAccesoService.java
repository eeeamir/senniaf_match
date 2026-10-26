package com.senniaf.match.service;

import com.senniaf.match.model.SesionAcceso;
import com.senniaf.match.model.Usuario;
import com.senniaf.match.repository.SesionAccesoRepository;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class SesionAccesoService {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final SesionAccesoRepository repository;
    public SesionAccesoService(SesionAccesoRepository repository) { this.repository = repository; }

    public String crear(Usuario usuario) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SesionAcceso s = new SesionAcceso();
        s.setTokenHash(hash(token));
        s.setUsuario(usuario);
        s.setExpira(LocalDateTime.now().plusHours(8));
        repository.save(s);
        return token;
    }

    public Optional<Usuario> autenticar(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return Optional.empty();
        String token = authorization.substring(7).trim();
        if (!token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return repository.findByTokenHash(hash(token))
                .filter(s -> s.getExpira().isAfter(LocalDateTime.now()))
                .map(SesionAcceso::getUsuario)
                .filter(Usuario::isVerificado);
    }

    @org.springframework.transaction.annotation.Transactional
    public void cerrar(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer "))
            repository.deleteByTokenHash(hash(authorization.substring(7).trim()));
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException("No se pudo validar la sesión"); }
    }
}
