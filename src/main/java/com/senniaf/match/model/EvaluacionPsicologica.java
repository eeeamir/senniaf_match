package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro independiente: no sustituye ni modifica los datos del perfil infantil. */
@Entity
@Table(name = "evaluaciones_psicologicas", indexes = @Index(name = "ix_evaluacion_expediente", columnList = "perfil_id"))
public class EvaluacionPsicologica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false) private PerfilNino expediente;
    @Column(nullable = false, length = 40) private String profesional;
    @Column(nullable = false) private LocalDateTime fecha;
    @Column(nullable = false, length = 5000) private String evaluacion;
    @Column(nullable = false, length = 5000) private String observaciones;
    @Column(nullable = false, length = 5000) private String recomendaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="revision_de_id", unique=true) private EvaluacionPsicologica revisionDe;
    private Integer numeroVersion;
    @Column(length=2000) private String motivoCorreccion;
    public EvaluacionPsicologica getRevisionDe(){return revisionDe;}
    public void setRevisionDe(EvaluacionPsicologica v){revisionDe=v;}
    public Integer getNumeroVersion(){return numeroVersion;}
    public void setNumeroVersion(Integer v){numeroVersion=v;}
    public String getMotivoCorreccion(){return motivoCorreccion;}
    public void setMotivoCorreccion(String v){motivoCorreccion=v;}

    @Column(name="contexto_evaluacion",columnDefinition="text") private String contextoEvaluacion;
    public String getContextoEvaluacion(){return contextoEvaluacion;}
    public void setContextoEvaluacion(String v){contextoEvaluacion=v;}
    @Column private Boolean sinDiagnostico;
    @Column(columnDefinition="text") private String areasObservadas;
    @Column(columnDefinition="text") private String hallazgosProfesionales;
    @Column(columnDefinition="text") private String diagnosticoProfesional;
    public Boolean getSinDiagnostico(){return sinDiagnostico;}
    public void setSinDiagnostico(Boolean v){sinDiagnostico=v;}
    public String getAreasObservadas(){return areasObservadas;}
    public void setAreasObservadas(String v){areasObservadas=v;}
    public String getHallazgosProfesionales(){return hallazgosProfesionales;}
    public void setHallazgosProfesionales(String v){hallazgosProfesionales=v;}
    public String getDiagnosticoProfesional(){return diagnosticoProfesional;}
    public void setDiagnosticoProfesional(String v){diagnosticoProfesional=v;}
    public Long getId() { return id; }
    public PerfilNino getExpediente() { return expediente; }
    public void setExpediente(PerfilNino value) { expediente = value; }
    public String getProfesional() { return profesional; }
    public void setProfesional(String value) { profesional = value; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime value) { fecha = value; }
    public String getEvaluacion() { return evaluacion; }
    public void setEvaluacion(String value) { evaluacion = value; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String value) { observaciones = value; }
    public String getRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(String value) { recomendaciones = value; }
}
