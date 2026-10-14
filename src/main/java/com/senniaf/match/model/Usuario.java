package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="usuarios", uniqueConstraints={
    @UniqueConstraint(name="uk_usuario_identificador", columnNames="identificador"),
    @UniqueConstraint(name="uk_usuario_cedula", columnNames="cedula"),
    @UniqueConstraint(name="uk_usuario_correo", columnNames="correo")
})
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=40) private String identificador;
    private String nombre;
    private String apellido;
    @Column(length=30) private String cedula;
    @Column(nullable=false, length=160) private String correo;
    @Column(nullable=false, length=100) private String passwordHash;
    @Column(nullable=false, length=30) private String rol;
    private boolean verificado;
    @Column(length=100) private String codigoVerificacionHash;
    private LocalDateTime codigoExpira;
    private int intentosVerificacion;
    private LocalDateTime fechaCreacion;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getIdentificador(){return identificador;} public void setIdentificador(String v){identificador=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getApellido(){return apellido;} public void setApellido(String v){apellido=v;}
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=v;}
    public String getCorreo(){return correo;} public void setCorreo(String v){correo=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getRol(){return rol;} public void setRol(String v){rol=v;}
    public boolean isVerificado(){return verificado;} public void setVerificado(boolean v){verificado=v;}
    public String getCodigoVerificacionHash(){return codigoVerificacionHash;} public void setCodigoVerificacionHash(String v){codigoVerificacionHash=v;}
    public LocalDateTime getCodigoExpira(){return codigoExpira;} public void setCodigoExpira(LocalDateTime v){codigoExpira=v;}
    public int getIntentosVerificacion(){return intentosVerificacion;} public void setIntentosVerificacion(int v){intentosVerificacion=v;}
    public LocalDateTime getFechaCreacion(){return fechaCreacion;} public void setFechaCreacion(LocalDateTime v){fechaCreacion=v;}
}
