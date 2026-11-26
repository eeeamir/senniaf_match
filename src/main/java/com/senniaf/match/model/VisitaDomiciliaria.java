package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "visitas_domiciliarias")
public class VisitaDomiciliaria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private Long vinculacionId;
    @Column(nullable = false, updatable = false, length = 40)
    private String autor;
    @Column(nullable = false, updatable = false, length = 25)
    private String rol;
    @Column(nullable = false, updatable = false)
    private Instant registrado;
    @Column(nullable = false, updatable = false)
    private UUID claveRegistro;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false)
    private LocalTime inicio;
    @Column(nullable = false)
    private LocalTime fin;
    @Version
    private long version;

    protected VisitaDomiciliaria() {}

    public VisitaDomiciliaria(Long vinculacionId, Usuario autor, Instant registrado, UUID claveRegistro) {
        this.vinculacionId = vinculacionId;
        this.autor = autor.getIdentificador();
        this.rol = autor.getRol();
        this.registrado = registrado;
        this.claveRegistro = claveRegistro;
    }

    public void actualizar(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        this.fecha = fecha;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getId() { return id; }
    public String getAutor() { return autor; }
    public String getRol() { return rol; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getInicio() { return inicio; }
    public LocalTime getFin() { return fin; }
    public long getVersion() { return version; }
}
