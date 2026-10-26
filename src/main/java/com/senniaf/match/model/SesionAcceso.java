package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_acceso", indexes = @Index(name = "ix_sesion_hash", columnList = "token_hash", unique = true))
public class SesionAcceso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) private Usuario usuario;
    @Column(nullable = false) private LocalDateTime expira;
    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { tokenHash = value; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario value) { usuario = value; }
    public LocalDateTime getExpira() { return expira; }
    public void setExpira(LocalDateTime value) { expira = value; }
}
