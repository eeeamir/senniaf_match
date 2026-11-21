package com.senniaf.match.model;

import jakarta.persistence.*;

/** Historial append-only: no expone setters ni operaciones de sustitución. */
@Entity
@Table(name="citas_historial")
public class CitaHistorial {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cita_id",nullable=false,updatable=false) private CitaVinculacion cita;
    @Column(nullable=false,length=40,updatable=false) private String actor;
    @Column(nullable=false,length=30,updatable=false) private String rol;
    @Column(nullable=false,length=40,updatable=false) private String profesional;
    @Column(nullable=false,length=30,updatable=false) private String accion;
    @Column(length=25,updatable=false) private String anterior;
    @Column(nullable=false,length=25,updatable=false) private String nuevo;
    @Column(length=40,updatable=false) private String motivoRespuesta;
    @Column(nullable=false,length=1000,updatable=false) private String mensaje;
    @Column(nullable=false,updatable=false) private java.time.LocalDateTime inicio;
    @Column(nullable=false,updatable=false) private java.time.LocalDateTime fin;
    @Column(nullable=false,updatable=false) private int duracion;
    @Column(nullable=false,updatable=false) private java.time.Instant fecha;
    protected CitaHistorial() {}
    public CitaHistorial(CitaVinculacion c,String actor,String rol,String accion,String anterior,String motivo) {
        cita=c;this.actor=actor;this.rol=rol;this.accion=accion;this.anterior=anterior;motivoRespuesta=motivo;
        profesional=c.getProfesional();nuevo=c.getEstado().name();mensaje=c.getMensaje();inicio=c.getInicio();fin=c.getFin();duracion=c.getDuracion();fecha=c.getActualizada();
    }
    public Long getId() { return id; }
    public CitaVinculacion getCita() { return cita; }
    public String getActor() { return actor; }
    public String getRol() { return rol; }
    public String getProfesional() { return profesional; }
    public String getAccion() { return accion; }
    public String getAnterior() { return anterior; }
    public String getNuevo() { return nuevo; }
    public String getMotivoRespuesta() { return motivoRespuesta; }
    public String getMensaje() { return mensaje; }
    public java.time.LocalDateTime getInicio() { return inicio; }
    public java.time.LocalDateTime getFin() { return fin; }
    public int getDuracion() { return duracion; }
    public java.time.Instant getFecha() { return fecha; }
}
