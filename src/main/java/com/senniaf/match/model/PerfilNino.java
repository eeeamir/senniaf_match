package com.senniaf.match.model;

import com.senniaf.match.model.enums.AreaHabilidad;
import com.senniaf.match.model.enums.Alergia;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.Interes;
import com.senniaf.match.model.enums.NivelEscolar;
import com.senniaf.match.model.enums.RasgoPersonalidad;
import com.senniaf.match.model.enums.RendimientoAcademico;
import com.senniaf.match.model.enums.RespuestaSiNoNoSabe;
import com.senniaf.match.model.enums.SiNo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación interna, pseudonimizada, del perfil integral de un niño
 * o niña en estado de adoptabilidad (sección 2.2). Se identifica solo por
 * codigoCaso; no guarda nombre ni ningún dato directamente identificable.
 *
 * A partir del Sprint 3, los campos que antes eran texto libre y admitían
 * un conjunto acotado de respuestas (escolaridad, rendimiento, hermanos,
 * etc.) pasan a listas cerradas (enum), y se agrega un bloque de
 * información médica (alergias, intolerancia a la lactosa, condiciones de
 * salud) pensado como datos estructurados que el motor de match podrá usar
 * más adelante, en vez de texto libre difícil de comparar.
 */
public class PerfilNino {

    private String codigoCaso;
    private Integer edad;

    // Escolaridad y rendimiento
    private NivelEscolar nivelEscolar;
    private String gradoEscolar;
    private RendimientoAcademico rendimientoAcademico;

    // Habilidades, gustos y personalidad
    private List<AreaHabilidad> habilidades;
    private String habilidadesDetalle;
    private List<Interes> gustos;
    private String gustosDetalle;
    private List<RasgoPersonalidad> personalidad;
    private String personalidadDetalle;

    // Información médica
    private List<Alergia> alergias;
    private String alergiasDetalle;
    private RespuestaSiNoNoSabe intoleranciaLactosa;
    private List<CondicionSalud> condicionesSalud;
    private String condicionesSaludDetalle;
    private String necesidadesApoyo;

    // Hermanos
    private SiNo tieneHermanos;
    private Integer cantidadHermanos;
    private String hermanosDetalle;

    private String contextoCultural;

    private String registradoPor;
    private LocalDateTime fechaRegistro;
    private boolean perfilCompleto;

    /**
     * true si el perfil declara al menos una alergia, condición de salud
     * o intolerancia relevante (distinta de "ninguna"/"no"). Sirve como
     * señal simple para que el Comité priorice la revisión de ese caso.
     */
    private boolean alertaMedica;

    public String getCodigoCaso() {
        return codigoCaso;
    }

    public void setCodigoCaso(String codigoCaso) {
        this.codigoCaso = codigoCaso;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public NivelEscolar getNivelEscolar() {
        return nivelEscolar;
    }

    public void setNivelEscolar(NivelEscolar nivelEscolar) {
        this.nivelEscolar = nivelEscolar;
    }

    public String getGradoEscolar() {
        return gradoEscolar;
    }

    public void setGradoEscolar(String gradoEscolar) {
        this.gradoEscolar = gradoEscolar;
    }

    public RendimientoAcademico getRendimientoAcademico() {
        return rendimientoAcademico;
    }

    public void setRendimientoAcademico(RendimientoAcademico rendimientoAcademico) {
        this.rendimientoAcademico = rendimientoAcademico;
    }

    public List<AreaHabilidad> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(List<AreaHabilidad> habilidades) {
        this.habilidades = habilidades;
    }

    public String getHabilidadesDetalle() {
        return habilidadesDetalle;
    }

    public void setHabilidadesDetalle(String habilidadesDetalle) {
        this.habilidadesDetalle = habilidadesDetalle;
    }

    public List<Interes> getGustos() {
        return gustos;
    }

    public void setGustos(List<Interes> gustos) {
        this.gustos = gustos;
    }

    public String getGustosDetalle() {
        return gustosDetalle;
    }

    public void setGustosDetalle(String gustosDetalle) {
        this.gustosDetalle = gustosDetalle;
    }

    public List<RasgoPersonalidad> getPersonalidad() {
        return personalidad;
    }

    public void setPersonalidad(List<RasgoPersonalidad> personalidad) {
        this.personalidad = personalidad;
    }

    public String getPersonalidadDetalle() {
        return personalidadDetalle;
    }

    public void setPersonalidadDetalle(String personalidadDetalle) {
        this.personalidadDetalle = personalidadDetalle;
    }

    public List<Alergia> getAlergias() {
        return alergias;
    }

    public void setAlergias(List<Alergia> alergias) {
        this.alergias = alergias;
    }

    public String getAlergiasDetalle() {
        return alergiasDetalle;
    }

    public void setAlergiasDetalle(String alergiasDetalle) {
        this.alergiasDetalle = alergiasDetalle;
    }

    public RespuestaSiNoNoSabe getIntoleranciaLactosa() {
        return intoleranciaLactosa;
    }

    public void setIntoleranciaLactosa(RespuestaSiNoNoSabe intoleranciaLactosa) {
        this.intoleranciaLactosa = intoleranciaLactosa;
    }

    public List<CondicionSalud> getCondicionesSalud() {
        return condicionesSalud;
    }

    public void setCondicionesSalud(List<CondicionSalud> condicionesSalud) {
        this.condicionesSalud = condicionesSalud;
    }

    public String getCondicionesSaludDetalle() {
        return condicionesSaludDetalle;
    }

    public void setCondicionesSaludDetalle(String condicionesSaludDetalle) {
        this.condicionesSaludDetalle = condicionesSaludDetalle;
    }

    public String getNecesidadesApoyo() {
        return necesidadesApoyo;
    }

    public void setNecesidadesApoyo(String necesidadesApoyo) {
        this.necesidadesApoyo = necesidadesApoyo;
    }

    public SiNo getTieneHermanos() {
        return tieneHermanos;
    }

    public void setTieneHermanos(SiNo tieneHermanos) {
        this.tieneHermanos = tieneHermanos;
    }

    public Integer getCantidadHermanos() {
        return cantidadHermanos;
    }

    public void setCantidadHermanos(Integer cantidadHermanos) {
        this.cantidadHermanos = cantidadHermanos;
    }

    public String getHermanosDetalle() {
        return hermanosDetalle;
    }

    public void setHermanosDetalle(String hermanosDetalle) {
        this.hermanosDetalle = hermanosDetalle;
    }

    public String getContextoCultural() {
        return contextoCultural;
    }

    public void setContextoCultural(String contextoCultural) {
        this.contextoCultural = contextoCultural;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isPerfilCompleto() {
        return perfilCompleto;
    }

    public void setPerfilCompleto(boolean perfilCompleto) {
        this.perfilCompleto = perfilCompleto;
    }

    public boolean isAlertaMedica() {
        return alertaMedica;
    }

    public void setAlertaMedica(boolean alertaMedica) {
        this.alertaMedica = alertaMedica;
    }
}
