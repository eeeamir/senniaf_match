package com.senniaf.match.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="registro_auditoria")
public class RegistroAuditoria {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private LocalDateTime fecha;
    @Column(nullable=false) private String actor;
    @Column(nullable=false) private String accion;
    @Column(nullable=false) private String codigoCaso;
    @Column(length=400) private String detalle;
    public Long getId(){return id;} public LocalDateTime getFecha(){return fecha;} public String getActor(){return actor;}
    public String getAccion(){return accion;} public String getCodigoCaso(){return codigoCaso;} public String getDetalle(){return detalle;}
    public void setFecha(LocalDateTime v){fecha=v;} public void setActor(String v){actor=v;} public void setAccion(String v){accion=v;}
    public void setCodigoCaso(String v){codigoCaso=v;} public void setDetalle(String v){detalle=v;}
}
