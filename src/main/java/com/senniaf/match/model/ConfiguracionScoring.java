package com.senniaf.match.model;
import jakarta.persistence.*;
@Entity @Table(name="configuracion_scoring")
public class ConfiguracionScoring {
    @Id private Long id=1L;
    private int version=1;
    private int edad=20, hermanos=25, salud=35, apoyo=15, logistica=5;
    public Long getId(){return id;} public int getVersion(){return version;} public void setVersion(int v){version=v;}
    public int getEdad(){return edad;} public void setEdad(int v){edad=v;}
    public int getHermanos(){return hermanos;} public void setHermanos(int v){hermanos=v;}
    public int getSalud(){return salud;} public void setSalud(int v){salud=v;}
    public int getApoyo(){return apoyo;} public void setApoyo(int v){apoyo=v;}
    public int getLogistica(){return logistica;} public void setLogistica(int v){logistica=v;}
}
