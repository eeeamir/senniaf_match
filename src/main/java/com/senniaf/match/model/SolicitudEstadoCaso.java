package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Una misma tramitación para desactivar o reactivar; no borra el expediente. */
@Entity
@Table(name = "solicitudes_estado_caso", indexes = @Index(name = "ix_solicitud_expediente", columnList = "perfil_id"))
public class SolicitudEstadoCaso {
    public enum Tipo { DESACTIVACION, REACTIVACION }
    public enum Categoria {
        ADOPCION_FORMALIZADA, CAMBIO_SITUACION, TRASLADO, SUSPENSION_TEMPORAL, OTRO,
        SITUACION_RESUELTA, REVISION_COMPLETADA, CONDICIONES_RESTABLECIDAS;
        public boolean admite(Tipo tipo) {
            return switch (this) {
                case OTRO -> true;
                case ADOPCION_FORMALIZADA, CAMBIO_SITUACION, TRASLADO, SUSPENSION_TEMPORAL
                        -> tipo == Tipo.DESACTIVACION;
                case SITUACION_RESUELTA, REVISION_COMPLETADA, CONDICIONES_RESTABLECIDAS
                        -> tipo == Tipo.REACTIVACION;
            };
        }
    }
    @Enumerated(EnumType.STRING) @Column(length = 40) private Categoria categoria;
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria valor) { categoria = valor; }
    public enum Estado { PENDIENTE, APROBADA, RECHAZADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false) private PerfilNino expediente;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Tipo tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Estado estado = Estado.PENDIENTE;
    @Column(nullable = false, length = 40) private String solicitante;
    @Column(nullable = false) private LocalDateTime fechaSolicitud;
    @Column(nullable = false, length = 2000) private String motivo;
    @Column(length = 40) private String resueltoPor;
    private LocalDateTime fechaResolucion;
    @Column(length = 2000) private String motivoResolucion;

    public Long getId() { return id; }
    public PerfilNino getExpediente() { return expediente; }
    public void setExpediente(PerfilNino v) { expediente = v; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo v) { tipo = v; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado v) { estado = v; }
    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String v) { solicitante = v; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime v) { fechaSolicitud = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { motivo = v; }
    public String getResueltoPor() { return resueltoPor; }
    public void setResueltoPor(String v) { resueltoPor = v; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime v) { fechaResolucion = v; }
    public String getMotivoResolucion() { return motivoResolucion; }
    public void setMotivoResolucion(String v) { motivoResolucion = v; }
}
