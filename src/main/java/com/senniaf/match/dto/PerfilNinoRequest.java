package com.senniaf.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos que el trabajador social o psicólogo envía para registrar el perfil
 * integral de un niño o niña en estado de adoptabilidad (sección 2.2 y HU01).
 *
 * No incluye nombre ni ningún dato que identifique directamente al niño o
 * niña: el sistema genera un código de caso pseudonimizado al guardar
 * (Art. 18, Ley 46 de 2013).
 */
public class PerfilNinoRequest {

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 0, message = "La edad no puede ser negativa")
    @Max(value = 17, message = "La edad debe corresponder a un menor de edad")
    private Integer edad;

    @NotBlank(message = "La escolaridad es obligatoria")
    private String escolaridad;

    private String rendimientoAcademico;

    private String habilidades;

    private String gustos;

    private String personalidad;

    @NotBlank(message = "Las necesidades del niño o niña son obligatorias")
    private String necesidades;

    private String hermanos;

    /**
     * Campo opcional de continuidad de identidad/cultura (sección 2.7):
     * vínculos con una comunidad, idioma o cultura que conviene preservar.
     * NO es un atributo de apariencia ni se usa como filtro (ver 2.6).
     */
    private String contextoCultural;

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
}
