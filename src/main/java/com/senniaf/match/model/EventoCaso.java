package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_casos")
public class EventoCaso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String codigoCaso;
    private String actor;
    private String accion;
    private LocalDateTime fecha;
    public Long getId(){return id;}
    public String getCodigoCaso(){return codigoCaso;} public void setCodigoCaso(String v){codigoCaso=v;}
    public String getActor(){return actor;} public void setActor(String v){actor=v;}
    public String getAccion(){return accion;} public void setAccion(String v){accion=v;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime v){fecha=v;}
}
