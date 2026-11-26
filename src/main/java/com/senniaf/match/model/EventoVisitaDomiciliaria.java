package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "visitas_domiciliarias_historial")
public class EventoVisitaDomiciliaria {
    public enum Accion { REGISTRADA, MODIFICADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_id", nullable = false, updatable = false)
    private VisitaDomiciliaria visita;
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

    protected EventoVisitaDomiciliaria() {}

    public EventoVisitaDomiciliaria(VisitaDomiciliaria visita, Usuario usuario, String nombreActor, Instant fecha,
                                    Accion accion, String cambios, String motivo) {
        this.visita = visita;
        this.actor = usuario.getIdentificador();
        this.rol = usuario.getRol();
        this.nombreActor = nombreActor;
        this.fecha = fecha;
        this.accion = accion;
        this.cambios = cambios;
        this.motivo = motivo;
    }

    public Long getVisitaId() { return visita.getId(); }
    public String getNombreActor() { return nombreActor; }
    public Instant getFecha() { return fecha; }
    public Accion getAccion() { return accion; }
    public String getCambios() { return cambios; }
    public String getMotivo() { return motivo; }
}
