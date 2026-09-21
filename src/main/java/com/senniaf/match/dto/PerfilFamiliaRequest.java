package com.senniaf.match.dto;

import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.DisponibilidadViaje;
import com.senniaf.match.model.enums.DisposicionHermanos;
import com.senniaf.match.model.enums.FuenteApoyo;
import com.senniaf.match.model.enums.ProvinciaPanama;
import com.senniaf.match.model.enums.TipoFamilia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Datos que la familia adoptante envía al completar la entrevista guiada
 * de idoneidad (HU02, PBI06/PBI07). Cada campo corresponde a una pregunta
 * reformulada de forma empática en el Sprint 0; el valor estructurado es
 * el que usa el motor de scoring, y cada "nota..." es el espacio de texto
 * libre que puede leer el Comité de Asignación Familiar.
 *
 * Las listas de condiciones de salud que la familia declara poder
 * acompañar pueden llegar vacías: a diferencia del perfil del niño/a
 * (donde una alergia o condición vacía sería un dato faltante), aquí una
 * lista vacía es una respuesta legítima ("aún no me siento en capacidad
 * de acompañar ninguna condición particular"), así que no se exige un
 * mínimo de selección.
 */
public class PerfilFamiliaRequest {

    @NotNull(message = "El tipo de estructura del hogar es obligatorio")
    private TipoFamilia tipoFamilia;

    private String notaEstructuraHogar;

    @NotEmpty(message = "Seleccione al menos una fuente de apoyo (o 'Ninguna por ahora')")
    private List<FuenteApoyo> redApoyo;

    private String notaRedApoyo;

    @NotNull(message = "La provincia o comarca donde vive la familia es obligatoria")
    private ProvinciaPanama provincia;

    @NotNull(message = "La disponibilidad para viajar o recibir visitas es obligatoria")
    private DisponibilidadViaje disponibilidadViaje;

    private String notaDistancia;

    @NotNull(message = "La edad mínima aceptada es obligatoria")
    @Min(value = 0, message = "La edad mínima no puede ser negativa")
    @Max(value = 17, message = "La edad mínima debe corresponder a un menor de edad")
    private Integer edadMinimaAceptada;

    @NotNull(message = "La edad máxima aceptada es obligatoria")
    @Min(value = 0, message = "La edad máxima no puede ser negativa")
    @Max(value = 17, message = "La edad máxima debe corresponder a un menor de edad")
    private Integer edadMaximaAceptada;

    private String notaRangoEdad;

    @NotNull(message = "La disposición a recibir grupos de hermanos es obligatoria")
    private DisposicionHermanos disposicionHermanos;

    private String notaHermanos;

    /**
     * Condiciones que la familia siente que puede acompañar hoy sin
     * apoyo adicional. Puede ir vacía (ver nota de clase).
     */
    private List<CondicionSalud> condicionesPuedeAcompanarHoy;

    /**
     * Condiciones que la familia podría acompañar si recibe capacitación
     * o apoyo adicional. Puede ir vacía (ver nota de clase).
     */
    private List<CondicionSalud> condicionesConApoyo;

    private String notaCondicionesSalud;

    /**
     * Cercanía con una cultura, idioma o comunidad que la familia
     * quisiera que el niño o niña pueda seguir conociendo. Texto libre a
     * propósito: no se presenta como catálogo ni filtro de
     * características (principio del rediseño humanizado del Sprint 0).
     */
    private String continuidadCultural;

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
}
