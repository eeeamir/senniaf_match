package com.senniaf.match.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="programaciones_pesos")
public class ProgramacionPesos {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,updatable=false,length=40) public String responsable;
 @Column(nullable=false,updatable=false,length=300) public String justificacion;
 @Column(nullable=false,updatable=false,columnDefinition="text") public String anteriores;
 @Column(nullable=false,updatable=false,columnDefinition="text") public String nuevos;
 @Column(nullable=false,updatable=false) public int versionAnterior;
 @Column(nullable=false,updatable=false) public int versionNueva;
 @Column(nullable=false,updatable=false) public Instant programadaEl;
 @Column(nullable=false,updatable=false) public Instant activarEl;
 public Instant activadaEl;
 @Column(nullable=false,length=20) public String estado="PENDIENTE";
}
