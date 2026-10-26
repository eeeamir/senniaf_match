package com.senniaf.match.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pesos_scoring")
public class PesosScoring {
    @Id private Long id = 1L;
    private int edad = 20, hermanos = 25, salud = 35, apoyo = 15, logistica = 5;
    private int version = 1;
    public Long getId(){return id;}
    public int getEdad(){return edad;} public void setEdad(int v){edad=v;}
    public int getHermanos(){return hermanos;} public void setHermanos(int v){hermanos=v;}
    public int getSalud(){return salud;} public void setSalud(int v){salud=v;}
    public int getApoyo(){return apoyo;} public void setApoyo(int v){apoyo=v;}
    public int getLogistica(){return logistica;} public void setLogistica(int v){logistica=v;}
    public int getVersion(){return version;} public void setVersion(int v){version=v;}
}
