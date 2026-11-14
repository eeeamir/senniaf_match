package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** La reserva pertenece a esta asignación; no altera la condición de los expedientes. */
@Entity
@Table(name="vinculaciones")
public class Vinculacion {
    public enum Estado { PENDIENTE_RESPUESTA, RESPUESTA_ACEPTADA, RESPUESTA_RECHAZADA_PENDIENTE_CIERRE }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(name="decision_id",nullable=false,unique=true) private DecisionComite decision;
    @ManyToOne(optional=false) @JoinColumn(name="nino_id",nullable=false) private PerfilNino nino;
    @ManyToOne(optional=false) @JoinColumn(name="familia_id",nullable=false) private PerfilFamilia familia;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=60) private Estado estado=Estado.PENDIENTE_RESPUESTA;
    @Column(nullable=false) private boolean reservaVigente=true;
    private LocalDateTime fechaRespuesta;
    @Column(length=40) private String respondidoPor;
    @Version private long version;
    public Long getId(){return id;}
    public DecisionComite getDecision(){return decision;} public void setDecision(DecisionComite v){decision=v;}
    public PerfilNino getNino(){return nino;} public void setNino(PerfilNino v){nino=v;}
    public PerfilFamilia getFamilia(){return familia;} public void setFamilia(PerfilFamilia v){familia=v;}
    public Estado getEstado(){return estado;} public void setEstado(Estado v){estado=v;}
    public boolean isReservaVigente(){return reservaVigente;}
    public LocalDateTime getFechaRespuesta(){return fechaRespuesta;} public void setFechaRespuesta(LocalDateTime v){fechaRespuesta=v;}
    public String getRespondidoPor(){return respondidoPor;} public void setRespondidoPor(String v){respondidoPor=v;}
}
