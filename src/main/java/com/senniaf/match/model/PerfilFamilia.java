package com.senniaf.match.model;

import com.senniaf.match.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="perfiles_familia", uniqueConstraints=@UniqueConstraint(name="uk_familia_codigo", columnNames="codigo_caso"))
public class PerfilFamilia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="codigo_caso", nullable=false, unique=true, length=20) private String codigoCaso;
    @Enumerated(EnumType.STRING) private TipoFamilia tipoFamilia;
    @Column(length=2000) private String notaEstructuraHogar;
    @ElementCollection(fetch=FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="familia_red_apoyo", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="fuente") private List<FuenteApoyo> redApoyo;
    @Column(length=2000) private String notaRedApoyo;
    @Enumerated(EnumType.STRING) private ProvinciaPanama provincia;
    @Enumerated(EnumType.STRING) private DisponibilidadViaje disponibilidadViaje;
    @Column(length=2000) private String notaDistancia;
    private Integer edadMinimaAceptada;
    private Integer edadMaximaAceptada;
    private String notaRangoEdad;
    @Enumerated(EnumType.STRING) private DisposicionHermanos disposicionHermanos;
    @Column(length=2000) private String notaHermanos;
    @ElementCollection(fetch=FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="familia_salud_hoy", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="condicion") private List<CondicionSalud> condicionesPuedeAcompanarHoy;
    @ElementCollection(fetch=FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="familia_salud_apoyo", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="condicion") private List<CondicionSalud> condicionesConApoyo;
    @Column(length=2000) private String notaCondicionesSalud;
    @Column(length=2000) private String continuidadCultural;
    @Column(name="registrado_por", nullable=false) private String registradoPor;
    private LocalDateTime fechaRegistro;
    private boolean perfilCompleto;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getCodigoCaso(){return codigoCaso;} public void setCodigoCaso(String v){codigoCaso=v;}
    public TipoFamilia getTipoFamilia(){return tipoFamilia;} public void setTipoFamilia(TipoFamilia v){tipoFamilia=v;}
    public String getNotaEstructuraHogar(){return notaEstructuraHogar;} public void setNotaEstructuraHogar(String v){notaEstructuraHogar=v;}
    public List<FuenteApoyo> getRedApoyo(){return redApoyo;} public void setRedApoyo(List<FuenteApoyo> v){redApoyo=v;}
    public String getNotaRedApoyo(){return notaRedApoyo;} public void setNotaRedApoyo(String v){notaRedApoyo=v;}
    public ProvinciaPanama getProvincia(){return provincia;} public void setProvincia(ProvinciaPanama v){provincia=v;}
    public DisponibilidadViaje getDisponibilidadViaje(){return disponibilidadViaje;} public void setDisponibilidadViaje(DisponibilidadViaje v){disponibilidadViaje=v;}
    public String getNotaDistancia(){return notaDistancia;} public void setNotaDistancia(String v){notaDistancia=v;}
    public Integer getEdadMinimaAceptada(){return edadMinimaAceptada;} public void setEdadMinimaAceptada(Integer v){edadMinimaAceptada=v;}
    public Integer getEdadMaximaAceptada(){return edadMaximaAceptada;} public void setEdadMaximaAceptada(Integer v){edadMaximaAceptada=v;}
    public String getNotaRangoEdad(){return notaRangoEdad;} public void setNotaRangoEdad(String v){notaRangoEdad=v;}
    public DisposicionHermanos getDisposicionHermanos(){return disposicionHermanos;} public void setDisposicionHermanos(DisposicionHermanos v){disposicionHermanos=v;}
    public String getNotaHermanos(){return notaHermanos;} public void setNotaHermanos(String v){notaHermanos=v;}
    public List<CondicionSalud> getCondicionesPuedeAcompanarHoy(){return condicionesPuedeAcompanarHoy;} public void setCondicionesPuedeAcompanarHoy(List<CondicionSalud> v){condicionesPuedeAcompanarHoy=v;}
    public List<CondicionSalud> getCondicionesConApoyo(){return condicionesConApoyo;} public void setCondicionesConApoyo(List<CondicionSalud> v){condicionesConApoyo=v;}
    public String getNotaCondicionesSalud(){return notaCondicionesSalud;} public void setNotaCondicionesSalud(String v){notaCondicionesSalud=v;}
    public String getContinuidadCultural(){return continuidadCultural;} public void setContinuidadCultural(String v){continuidadCultural=v;}
    public String getRegistradoPor(){return registradoPor;} public void setRegistradoPor(String v){registradoPor=v;}
    public LocalDateTime getFechaRegistro(){return fechaRegistro;} public void setFechaRegistro(LocalDateTime v){fechaRegistro=v;}
    public boolean isPerfilCompleto(){return perfilCompleto;} public void setPerfilCompleto(boolean v){perfilCompleto=v;}
}
