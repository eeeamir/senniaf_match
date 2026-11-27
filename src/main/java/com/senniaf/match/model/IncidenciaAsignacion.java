package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencias_asignacion")
public class IncidenciaAsignacion {
    public enum Motivo { DESISTIMIENTO_FAMILIA, RECHAZO_NINO, OTRO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, updatable = false)
    private Vinculacion vinculacion;
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false, updatable = false)
    private Motivo motivo;
    @Column(length = 2000, nullable = false, updatable = false)
    private String justificacion;
    @Column(length = 40, nullable = false, updatable = false)
    private String registradoPor;
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
    private Boolean procedeRevocacion;
    @Column(length = 2000)
    private String fundamentoRevision;
    @Column(length = 40)
    private String revisadoPor;
    private LocalDateTime fechaRevision;

    protected IncidenciaAsignacion() {}

    public IncidenciaAsignacion(Vinculacion vinculacion, Motivo motivo, String justificacion, String actor) {
        this.vinculacion = vinculacion;
        this.motivo = motivo;
        this.justificacion = justificacion;
        this.registradoPor = actor;
        this.fechaRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Motivo getMotivo() { return motivo; }
    public String getJustificacion() { return justificacion; }
    public String getRegistradoPor() { return registradoPor; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public Boolean getProcedeRevocacion() { return procedeRevocacion; }
    public String getFundamentoRevision() { return fundamentoRevision; }
    public String getRevisadoPor() { return revisadoPor; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }

    public void revisar(boolean procede, String fundamento, String actor) {
        procedeRevocacion = procede;
        fundamentoRevision = fundamento;
        revisadoPor = actor;
        fechaRevision = LocalDateTime.now();
    }
}
