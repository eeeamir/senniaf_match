package com.senniaf.match.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "decisiones_comite")
public class DecisionComite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 24) private String codigoNino;
    @Column(nullable = false, length = 24) private String codigoFamilia;
    @Column(nullable = false, length = 18) private String decision;
    @Column(nullable = false, length = 2000) private String motivo;
    @Column(nullable = false, length = 40) private String funcionario;
    private LocalDateTime fecha;
    private int puntaje;
    private int versionPesos;

    public Long getId(){return id;}
    public String getCodigoNino(){return codigoNino;} public void setCodigoNino(String v){codigoNino=v;}
    public String getCodigoFamilia(){return codigoFamilia;} public void setCodigoFamilia(String v){codigoFamilia=v;}
    public String getDecision(){return decision;} public void setDecision(String v){decision=v;}
    public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v;}
    public String getFuncionario(){return funcionario;} public void setFuncionario(String v){funcionario=v;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime v){fecha=v;}
    public int getPuntaje(){return puntaje;} public void setPuntaje(int v){puntaje=v;}
    public int getVersionPesos(){return versionPesos;} public void setVersionPesos(int v){versionPesos=v;}
}
