package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.*;

/** Constancia profesional y revisión posterior, conservando el informe original. */
@Entity
@Table(name="interrupciones_convivencia")
public class InterrupcionConvivencia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(name="vinculacion_id",nullable=false,unique=true,updatable=false) private Vinculacion vinculacion;
    @Column(nullable=false,updatable=false) private LocalDate fechaInterrupcion;
    @Column(nullable=false,length=200,updatable=false) private String referenciaActuacion;
    @Column(nullable=false,length=2000,updatable=false) private String justificacion;
    @Column(nullable=false,length=40,updatable=false) private String registradoPor;
    @Column(nullable=false,length=25,updatable=false) private String rolProfesional;
    @Column(nullable=false,updatable=false) private LocalDateTime fechaRegistro;
    @Column(length=2000) private String revisionJustificacion;
    @Column(length=40) private String revisadoPor;
    private LocalDateTime fechaRevision;
    @Version private long version;
    public Long getId(){return id;}
    public Vinculacion getVinculacion(){return vinculacion;} public void setVinculacion(Vinculacion v){vinculacion=v;}
    public LocalDate getFechaInterrupcion(){return fechaInterrupcion;} public void setFechaInterrupcion(LocalDate v){fechaInterrupcion=v;}
    public String getReferenciaActuacion(){return referenciaActuacion;} public void setReferenciaActuacion(String v){referenciaActuacion=v;}
    public String getJustificacion(){return justificacion;} public void setJustificacion(String v){justificacion=v;}
    public String getRegistradoPor(){return registradoPor;} public void setRegistradoPor(String v){registradoPor=v;}
    public String getRolProfesional(){return rolProfesional;} public void setRolProfesional(String v){rolProfesional=v;}
    public LocalDateTime getFechaRegistro(){return fechaRegistro;} public void setFechaRegistro(LocalDateTime v){fechaRegistro=v;}
    public String getRevisionJustificacion(){return revisionJustificacion;} public void setRevisionJustificacion(String v){revisionJustificacion=v;}
    public String getRevisadoPor(){return revisadoPor;} public void setRevisadoPor(String v){revisadoPor=v;}
    public LocalDateTime getFechaRevision(){return fechaRevision;} public void setFechaRevision(LocalDateTime v){fechaRevision=v;}
}
