package com.senniaf.match.model;

import com.senniaf.match.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "perfiles_nino", uniqueConstraints = @UniqueConstraint(name = "uk_nino_codigo", columnNames = "codigo_caso"))
public class PerfilNino {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="codigo_caso", nullable=false, unique=true, length=20) private String codigoCaso;
    private Integer edad;
    @Enumerated(EnumType.STRING) private NivelEscolar nivelEscolar;
    private String gradoEscolar;
    @Enumerated(EnumType.STRING) private RendimientoAcademico rendimientoAcademico;

    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="nino_habilidades", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="habilidad") private List<AreaHabilidad> habilidades;
    private String habilidadesDetalle;
    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="nino_gustos", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="gusto") private List<Interes> gustos;
    private String gustosDetalle;
    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="nino_personalidad", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="rasgo") private List<RasgoPersonalidad> personalidad;
    private String personalidadDetalle;
    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="nino_alergias", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="alergia") private List<Alergia> alergias;
    private String alergiasDetalle;
    @Enumerated(EnumType.STRING) private RespuestaSiNoNoSabe intoleranciaLactosa;
    @ElementCollection(fetch = FetchType.EAGER) @Enumerated(EnumType.STRING)
    @CollectionTable(name="nino_condiciones_salud", joinColumns=@JoinColumn(name="perfil_id"))
    @Column(name="condicion") private List<CondicionSalud> condicionesSalud;
    private String condicionesSaludDetalle;
    @Column(length=2000) private String necesidadesApoyo;
    @Enumerated(EnumType.STRING) private SiNo tieneHermanos;
    private Integer cantidadHermanos;
    private String hermanosDetalle;
    @Column(length=2000) private String contextoCultural;
    private String registradoPor;
    private LocalDateTime fechaRegistro;
    private boolean perfilCompleto;
    private boolean alertaMedica;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getCodigoCaso(){return codigoCaso;} public void setCodigoCaso(String v){codigoCaso=v;}
    public Integer getEdad(){return edad;} public void setEdad(Integer v){edad=v;}
    public NivelEscolar getNivelEscolar(){return nivelEscolar;} public void setNivelEscolar(NivelEscolar v){nivelEscolar=v;}
    public String getGradoEscolar(){return gradoEscolar;} public void setGradoEscolar(String v){gradoEscolar=v;}
    public RendimientoAcademico getRendimientoAcademico(){return rendimientoAcademico;} public void setRendimientoAcademico(RendimientoAcademico v){rendimientoAcademico=v;}
    public List<AreaHabilidad> getHabilidades(){return habilidades;} public void setHabilidades(List<AreaHabilidad> v){habilidades=v;}
    public String getHabilidadesDetalle(){return habilidadesDetalle;} public void setHabilidadesDetalle(String v){habilidadesDetalle=v;}
    public List<Interes> getGustos(){return gustos;} public void setGustos(List<Interes> v){gustos=v;}
    public String getGustosDetalle(){return gustosDetalle;} public void setGustosDetalle(String v){gustosDetalle=v;}
    public List<RasgoPersonalidad> getPersonalidad(){return personalidad;} public void setPersonalidad(List<RasgoPersonalidad> v){personalidad=v;}
    public String getPersonalidadDetalle(){return personalidadDetalle;} public void setPersonalidadDetalle(String v){personalidadDetalle=v;}
    public List<Alergia> getAlergias(){return alergias;} public void setAlergias(List<Alergia> v){alergias=v;}
    public String getAlergiasDetalle(){return alergiasDetalle;} public void setAlergiasDetalle(String v){alergiasDetalle=v;}
    public RespuestaSiNoNoSabe getIntoleranciaLactosa(){return intoleranciaLactosa;} public void setIntoleranciaLactosa(RespuestaSiNoNoSabe v){intoleranciaLactosa=v;}
    public List<CondicionSalud> getCondicionesSalud(){return condicionesSalud;} public void setCondicionesSalud(List<CondicionSalud> v){condicionesSalud=v;}
    public String getCondicionesSaludDetalle(){return condicionesSaludDetalle;} public void setCondicionesSaludDetalle(String v){condicionesSaludDetalle=v;}
    public String getNecesidadesApoyo(){return necesidadesApoyo;} public void setNecesidadesApoyo(String v){necesidadesApoyo=v;}
    public SiNo getTieneHermanos(){return tieneHermanos;} public void setTieneHermanos(SiNo v){tieneHermanos=v;}
    public Integer getCantidadHermanos(){return cantidadHermanos;} public void setCantidadHermanos(Integer v){cantidadHermanos=v;}
    public String getHermanosDetalle(){return hermanosDetalle;} public void setHermanosDetalle(String v){hermanosDetalle=v;}
    public String getContextoCultural(){return contextoCultural;} public void setContextoCultural(String v){contextoCultural=v;}
    public String getRegistradoPor(){return registradoPor;} public void setRegistradoPor(String v){registradoPor=v;}
    public LocalDateTime getFechaRegistro(){return fechaRegistro;} public void setFechaRegistro(LocalDateTime v){fechaRegistro=v;}
    public boolean isPerfilCompleto(){return perfilCompleto;} public void setPerfilCompleto(boolean v){perfilCompleto=v;}
    public boolean isAlertaMedica(){return alertaMedica;} public void setAlertaMedica(boolean v){alertaMedica=v;}
}
