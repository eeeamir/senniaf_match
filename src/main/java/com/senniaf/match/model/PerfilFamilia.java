package com.senniaf.match.model;

import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.DisponibilidadViaje;
import com.senniaf.match.model.enums.DisposicionHermanos;
import com.senniaf.match.model.enums.FuenteApoyo;
import com.senniaf.match.model.enums.ProvinciaPanama;
import com.senniaf.match.model.enums.TipoFamilia;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación interna, pseudonimizada, del perfil de idoneidad de una
 * familia adoptante (PBI06/PBI07, HU02). Se identifica solo por
 * codigoCaso; no guarda nombres ni datos de contacto directos.
 *
 * Los campos siguen el orden y el diseño de la "entrevista humanizada"
 * del Sprint 0: cada pregunta empática guarda un valor estructurado que
 * el motor de scoring podrá usar, más una nota de texto libre que el
 * Comité puede leer aunque el motor no la use directamente. El orden de
 * los campos respeta el principio de la entrevista de empezar por
 * preguntas de conexión (estructura del hogar) antes de las más
 * sensibles (salud, hermanos).
 *
 * Las condiciones de salud que la familia declara poder acompañar
 * reutilizan el mismo enum CondicionSalud del perfil del niño/a, para
 * que ambos lados puedan compararse directamente en el futuro motor de
 * match (PBI08).
 */
public class PerfilFamilia {

    private String codigoCaso;

    // Estructura del hogar
    private TipoFamilia tipoFamilia;
    private String notaEstructuraHogar;

    // Red de apoyo
    private List<FuenteApoyo> redApoyo;
    private String notaRedApoyo;

    // Distancia / región
    private ProvinciaPanama provincia;
    private DisponibilidadViaje disponibilidadViaje;
    private String notaDistancia;

    // Rango de edad aceptado
    private Integer edadMinimaAceptada;
    private Integer edadMaximaAceptada;
    private String notaRangoEdad;

    // Disposición a grupos de hermanos
    private DisposicionHermanos disposicionHermanos;
    private String notaHermanos;

    // Condiciones de salud / desarrollo aceptadas (escala, no binario)
    private List<CondicionSalud> condicionesPuedeAcompanarHoy;
    private List<CondicionSalud> condicionesConApoyo;
    private String notaCondicionesSalud;

    // Continuidad de identidad / cultura (texto libre a propósito)
    private String continuidadCultural;

    private String registradoPor;
    private LocalDateTime fechaRegistro;
    private boolean perfilCompleto;

    public String getCodigoCaso() {
        return codigoCaso;
    }

    public void setCodigoCaso(String codigoCaso) {
        this.codigoCaso = codigoCaso;
    }

    public TipoFamilia getTipoFamilia() {
        return tipoFamilia;
    }

    public void setTipoFamilia(TipoFamilia tipoFamilia) {
        this.tipoFamilia = tipoFamilia;
    }

    public String getNotaEstructuraHogar() {
        return notaEstructuraHogar;
    }

    public void setNotaEstructuraHogar(String notaEstructuraHogar) {
        this.notaEstructuraHogar = notaEstructuraHogar;
    }

    public List<FuenteApoyo> getRedApoyo() {
        return redApoyo;
    }

    public void setRedApoyo(List<FuenteApoyo> redApoyo) {
        this.redApoyo = redApoyo;
    }

    public String getNotaRedApoyo() {
        return notaRedApoyo;
    }

    public void setNotaRedApoyo(String notaRedApoyo) {
        this.notaRedApoyo = notaRedApoyo;
    }

    public ProvinciaPanama getProvincia() {
        return provincia;
    }

    public void setProvincia(ProvinciaPanama provincia) {
        this.provincia = provincia;
    }

    public DisponibilidadViaje getDisponibilidadViaje() {
        return disponibilidadViaje;
    }

    public void setDisponibilidadViaje(DisponibilidadViaje disponibilidadViaje) {
        this.disponibilidadViaje = disponibilidadViaje;
    }

    public String getNotaDistancia() {
        return notaDistancia;
    }

    public void setNotaDistancia(String notaDistancia) {
        this.notaDistancia = notaDistancia;
    }

    public Integer getEdadMinimaAceptada() {
        return edadMinimaAceptada;
    }

    public void setEdadMinimaAceptada(Integer edadMinimaAceptada) {
        this.edadMinimaAceptada = edadMinimaAceptada;
    }

    public Integer getEdadMaximaAceptada() {
        return edadMaximaAceptada;
    }

    public void setEdadMaximaAceptada(Integer edadMaximaAceptada) {
        this.edadMaximaAceptada = edadMaximaAceptada;
    }

    public String getNotaRangoEdad() {
        return notaRangoEdad;
    }

    public void setNotaRangoEdad(String notaRangoEdad) {
        this.notaRangoEdad = notaRangoEdad;
    }

    public DisposicionHermanos getDisposicionHermanos() {
        return disposicionHermanos;
    }

    public void setDisposicionHermanos(DisposicionHermanos disposicionHermanos) {
        this.disposicionHermanos = disposicionHermanos;
    }

    public String getNotaHermanos() {
        return notaHermanos;
    }

    public void setNotaHermanos(String notaHermanos) {
        this.notaHermanos = notaHermanos;
    }

    public List<CondicionSalud> getCondicionesPuedeAcompanarHoy() {
        return condicionesPuedeAcompanarHoy;
    }

    public void setCondicionesPuedeAcompanarHoy(List<CondicionSalud> condicionesPuedeAcompanarHoy) {
        this.condicionesPuedeAcompanarHoy = condicionesPuedeAcompanarHoy;
    }

    public List<CondicionSalud> getCondicionesConApoyo() {
        return condicionesConApoyo;
    }

    public void setCondicionesConApoyo(List<CondicionSalud> condicionesConApoyo) {
        this.condicionesConApoyo = condicionesConApoyo;
    }

    public String getNotaCondicionesSalud() {
        return notaCondicionesSalud;
    }

    public void setNotaCondicionesSalud(String notaCondicionesSalud) {
        this.notaCondicionesSalud = notaCondicionesSalud;
    }

    public String getContinuidadCultural() {
        return continuidadCultural;
    }

    public void setContinuidadCultural(String continuidadCultural) {
        this.continuidadCultural = continuidadCultural;
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
