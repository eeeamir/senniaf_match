package com.senniaf.match.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="sesiones_acceso")
public class SesionAcceso {
    @Id @Column(length=64) private String tokenHash;
    @ManyToOne(optional=false, fetch=FetchType.EAGER) private Usuario usuario;
    private LocalDateTime expira;
    public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
    public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario v){usuario=v;}
    public LocalDateTime getExpira(){return expira;} public void setExpira(LocalDateTime v){expira=v;}
}
