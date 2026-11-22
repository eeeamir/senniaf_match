package com.senniaf.match.model;

import jakarta.persistence.*;

@Entity
@Table(name="citas_vinculacion")
public class CitaVinculacion {
    public enum Estado { PENDIENTE, CONFIRMADA, CAMBIO_PROPUESTO, RECHAZADA, CANCELADA, REALIZADA }
    public enum Motivo { ORIENTACION, SEGUIMIENTO, ENCUENTRO, OBSERVACION, DUDAS, OTRO }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="vinculacion_id",nullable=false) private Vinculacion vinculacion;
    @Column(nullable=false,length=40) private String profesional;
    @Column(nullable=false,length=40,updatable=false) private String creadoPor;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30,updatable=false) private Motivo motivo;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=25) private Estado estado;
    @Column(nullable=false) private java.time.LocalDateTime inicio;
    @Column(nullable=false) private java.time.LocalDateTime fin;
    @Column(nullable=false) private int duracion;
    @Column(nullable=false,length=1000,updatable=false) private String mensajeInicial;
    @Column(nullable=false,length=1000) private String mensaje;
    @Column(length=1000) private String respuesta;
    @Column(nullable=false,length=30) private String ultimoRol;
    @Column(nullable=false,updatable=false) private java.time.Instant creada;
    @Column(nullable=false) private java.time.Instant actualizada;
    @Version private long version;
    public Long getId() { return id; }
    public Vinculacion getVinculacion() { return vinculacion; }
    public void setVinculacion(Vinculacion valor) { vinculacion=valor; }
    public String getProfesional() { return profesional; }
    public void setProfesional(String valor) { profesional=valor; }
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String valor) { creadoPor=valor; }
    public Motivo getMotivo() { return motivo; }
    public void setMotivo(Motivo valor) { motivo=valor; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado valor) { estado=valor; }
    public java.time.LocalDateTime getInicio() { return inicio; }
    public void setInicio(java.time.LocalDateTime valor) { inicio=valor; }
    public java.time.LocalDateTime getFin() { return fin; }
    public void setFin(java.time.LocalDateTime valor) { fin=valor; }
    public int getDuracion() { return duracion; }
    public void setDuracion(int valor) { duracion=valor; }
    public String getMensajeInicial() { return mensajeInicial; }
    public void setMensajeInicial(String valor) { mensajeInicial=valor; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String valor) { mensaje=valor; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String valor) { respuesta=valor; }
    public String getUltimoRol() { return ultimoRol; }
    public void setUltimoRol(String valor) { ultimoRol=valor; }
    public java.time.Instant getCreada() { return creada; }
    public void setCreada(java.time.Instant valor) { creada=valor; }
    public java.time.Instant getActualizada() { return actualizada; }
    public void setActualizada(java.time.Instant valor) { actualizada=valor; }
    public long getVersion() { return version; }
}
