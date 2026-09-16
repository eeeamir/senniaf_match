package com.senniaf.match.model;

import java.time.LocalDateTime;

/**
 * Representación interna, pseudonimizada, del perfil integral de un niño
 * o niña en estado de adoptabilidad (sección 2.2). Se identifica solo por
 * codigoCaso; no guarda nombre ni ningún dato directamente identificable.
 */
public class PerfilNino {

    private String codigoCaso;
    private Integer edad;
    private String escolaridad;
    private String rendimientoAcademico;
    private String habilidades;
    private String gustos;
    private String personalidad;
    private String necesidades;
    private String hermanos;
    private String contextoCultural;
    private String registradoPor;
    private LocalDateTime fechaRegistro;
    private boolean perfilCompleto;

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

    public String getEscolaridad() {
        return escolaridad;
    }

    public void setEscolaridad(String escolaridad) {
        this.escolaridad = escolaridad;
    }

    public String getRendimientoAcademico() {
        return rendimientoAcademico;
    }

    public void setRendimientoAcademico(String rendimientoAcademico) {
        this.rendimientoAcademico = rendimientoAcademico;
    }

    public String getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(String habilidades) {
        this.habilidades = habilidades;
    }

    public String getGustos() {
        return gustos;
    }

    public void setGustos(String gustos) {
        this.gustos = gustos;
    }

    public String getPersonalidad() {
        return personalidad;
    }

    public void setPersonalidad(String personalidad) {
        this.personalidad = personalidad;
    }

    public String getNecesidades() {
        return necesidades;
    }

    public void setNecesidades(String necesidades) {
        this.necesidades = necesidades;
    }

    public String getHermanos() {
        return hermanos;
    }

    public void setHermanos(String hermanos) {
        this.hermanos = hermanos;
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
}
