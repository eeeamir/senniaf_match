package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "seguimientos_vinculacion")
public class SeguimientoVinculacion {
    public enum Etapa { ENCUENTROS_PREVIOS, CONVIVENCIA_PREADOPTIVA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private Long vinculacionId;
    @Column(nullable = false, updatable = false, length = 40)
    private String autor;
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String nombreAutor;
    @Column(nullable = false, updatable = false, length = 25)
    private String rol;
    @Column(nullable = false, updatable = false)
    private UUID claveRegistro;
    @Column(nullable = false, updatable = false)
    private Instant registrado;
    @Column(nullable = false)
    private LocalDate fechaAtencion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Etapa etapa;
    @Column(nullable = false, columnDefinition = "text")
    private String contenido;
    @Version
    private long version;

    protected SeguimientoVinculacion() {}

    public SeguimientoVinculacion(Long vinculacionId, Usuario usuario, String nombreCompleto, UUID claveRegistro, Instant registrado) {
        this.vinculacionId = vinculacionId;
        this.autor = usuario.getIdentificador();
        this.nombreAutor = nombreCompleto;
        this.rol = usuario.getRol();
        this.claveRegistro = claveRegistro;
        this.registrado = registrado;
    }

    public void actualizar(LocalDate fechaAtencion, Etapa etapa, String contenido) {
        this.fechaAtencion = fechaAtencion;
        this.etapa = etapa;
        this.contenido = contenido;
    }

    public Long getId() { return id; }
    public Long getVinculacionId() { return vinculacionId; }
    public String getAutor() { return autor; }
    public String getNombreAutor() { return nombreAutor; }
    public String getRol() { return rol; }
    public Instant getRegistrado() { return registrado; }
    public LocalDate getFechaAtencion() { return fechaAtencion; }
    public Etapa getEtapa() { return etapa; }
    public String getContenido() { return contenido; }
    public long getVersion() { return version; }
}
