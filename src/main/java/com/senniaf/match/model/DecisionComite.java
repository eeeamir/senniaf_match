package com.senniaf.match.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="decisiones_comite")
public class DecisionComite {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String codigoNino;
 @Column(nullable=false) private String codigoFamilia;
 @Column(nullable=false) private String decision;
 @Column(nullable=false,length=2000) private String motivo;
 @Column(nullable=false) private String actor;
 @Column(nullable=false) private LocalDateTime fecha;
 private int puntaje;private int versionScoring;
 @Column(length=5000) private String factoresSnapshot;
 public String getFactoresSnapshot(){return factoresSnapshot;}public void setFactoresSnapshot(String v){factoresSnapshot=v;}
 public Long getId(){return id;}
 public String getCodigoNino(){return codigoNino;} public void setCodigoNino(String v){codigoNino=v;}
 public String getCodigoFamilia(){return codigoFamilia;} public void setCodigoFamilia(String v){codigoFamilia=v;}
 public String getDecision(){return decision;} public void setDecision(String v){decision=v;}
 public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v;}
 public String getActor(){return actor;} public void setActor(String v){actor=v;}
 public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime v){fecha=v;}
 public int getPuntaje(){return puntaje;} public void setPuntaje(int v){puntaje=v;}
 public int getVersionScoring(){return versionScoring;} public void setVersionScoring(int v){versionScoring=v;}
}
