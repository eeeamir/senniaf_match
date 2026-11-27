package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_reincorporacion")
public class SolicitudReincorporacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculacion_id", nullable = false, unique = true, updatable = false)
    private Vinculacion vinculacion;
    @Column(nullable = false, updatable = false)
    private LocalDateTime solicitada;
    private LocalDateTime entrevistaActualizada;
    private Boolean autorizada;
    private LocalDateTime resuelta;
    @Column(length = 40)
    private String resueltoPor;
    @Column(length = 2000)
    private String fundamento;

    protected SolicitudReincorporacion() {}

    public SolicitudReincorporacion(Vinculacion vinculacion) {
        this.vinculacion = vinculacion;
        solicitada = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Vinculacion getVinculacion() { return vinculacion; }
    public LocalDateTime getSolicitada() { return solicitada; }
    public LocalDateTime getEntrevistaActualizada() { return entrevistaActualizada; }
    public Boolean getAutorizada() { return autorizada; }
    public LocalDateTime getResuelta() { return resuelta; }
    public String getResueltoPor() { return resueltoPor; }
    public String getFundamento() { return fundamento; }

    public void confirmarEntrevista() { entrevistaActualizada = LocalDateTime.now(); }

    public void resolver(boolean autorizada, String actor, String fundamento) {
        this.autorizada = autorizada;
        this.resueltoPor = actor;
        this.fundamento = fundamento;
        this.resuelta = LocalDateTime.now();
    }
}
