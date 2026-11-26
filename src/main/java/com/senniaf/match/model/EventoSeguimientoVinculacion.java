package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "seguimientos_vinculacion_historial")
public class EventoSeguimientoVinculacion {
    public enum Accion { REGISTRADO, MODIFICADO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private Long vinculacionId;
    @Column(nullable = false, updatable = false)
    private Long seguimientoId;
    @Column(nullable = false, updatable = false, length = 40)
    private String actor;
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String nombreActor;
    @Column(nullable = false, updatable = false, length = 25)
    private String rol;
    @Column(nullable = false, updatable = false)
    private Instant fecha;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 15)
    private Accion accion;
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String cambios;
    @Column(updatable = false, length = 1000)
    private String motivo;

    protected EventoSeguimientoVinculacion() {}

    public EventoSeguimientoVinculacion(SeguimientoVinculacion seguimiento, Usuario usuario, String nombreCompleto, Instant fecha,
                                        Accion accion, String cambios, String motivo) {
        vinculacionId = seguimiento.getVinculacionId();
        seguimientoId = seguimiento.getId();
        actor = usuario.getIdentificador();
        nombreActor = nombreCompleto;
        rol = usuario.getRol();
        this.fecha = fecha;
        this.accion = accion;
        this.cambios = cambios;
        this.motivo = motivo;
    }

    public Long getSeguimientoId() { return seguimientoId; }
    public String getNombreActor() { return nombreActor; }
    public String getRol() { return rol; }
    public Instant getFecha() { return fecha; }
    public Accion getAccion() { return accion; }
    public String getCambios() { return cambios; }
    public String getMotivo() { return motivo; }
}
