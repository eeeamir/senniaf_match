package com.senniaf.match.dto;

import com.senniaf.match.model.enums.AreaHabilidad;
import com.senniaf.match.model.enums.Alergia;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.Interes;
import com.senniaf.match.model.enums.NivelEscolar;
import com.senniaf.match.model.enums.RasgoPersonalidad;
import com.senniaf.match.model.enums.RendimientoAcademico;
import com.senniaf.match.model.enums.RespuestaSiNoNoSabe;
import com.senniaf.match.model.enums.SiNo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Datos que el trabajador social o psicólogo envía para registrar el perfil
 * integral de un niño o niña en estado de adoptabilidad (sección 2.2 y HU01).
 *
 * No incluye nombre ni ningún dato que identifique directamente al niño o
 * niña: el sistema genera un código de caso pseudonimizado al guardar
 * (Art. 18, Ley 46 de 2013).
 *
 * Desde el Sprint 3, los campos que antes eran texto libre pero en realidad
 * representan un conjunto acotado de respuestas (escolaridad, rendimiento,
 * habilidades, gustos, personalidad, hermanos) se validan como listas
 * cerradas (enum) en vez de String. Cada lista de selección múltiple exige
 * al menos un valor: para "ninguno aplica" existe una opción explícita
 * (NINGUNA_CONOCIDA, NINGUNO_IDENTIFICADO, etc.) en vez de dejar el campo
 * vacío, así se distingue "no tiene" de "no se preguntó". Cuando se elige
 * "OTRA/OTRO" el campo de texto libre correspondiente ("...Detalle") queda
 * disponible para el matiz que la lista no cubre.
 *
 * También se agrega el bloque de información médica solicitado por el
 * equipo: alergias, intolerancia a la lactosa y condiciones de salud,
 * como datos estructurados en vez de mezclarlos dentro del campo genérico
 * "necesidades".
 */
public class PerfilNinoRequest {

    private String numeroExpedienteInstitucional;
    public String getNumeroExpedienteInstitucional() { return numeroExpedienteInstitucional; }
    public void setNumeroExpedienteInstitucional(String valor) { this.numeroExpedienteInstitucional=valor; }

    /** Campo legado para compatibilidad de clientes anteriores; ya no se exige ni concede permisos. */
    private Boolean asignacionInstitucionalConfirmada;

    public Boolean getAsignacionInstitucionalConfirmada() { return asignacionInstitucionalConfirmada; }
    public void setAsignacionInstitucionalConfirmada(Boolean valor) { this.asignacionInstitucionalConfirmada = valor; }

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 0, message = "La edad no puede ser negativa")
    @Max(value = 17, message = "La edad debe corresponder a un menor de edad")
    @JsonDeserialize(using=EnteroEstricto.class)
    private Integer edad;

    @NotNull(message = "El nivel escolar es obligatorio")
    private NivelEscolar nivelEscolar;

    /**
     * Grado o año específico dentro del nivel escolar (ej. "5to grado").
     * Texto libre y opcional: es un detalle informativo, no un criterio
     * de comparación del motor de match.
     */
    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String gradoEscolar;

    @NotNull(message = "El rendimiento académico es obligatorio")
    private RendimientoAcademico rendimientoAcademico;

    @NotEmpty(message = "Seleccione al menos un área de habilidad (o 'Ninguna identificada')")
    private List<@NotNull AreaHabilidad> habilidades;

    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String habilidadesDetalle;

    @NotEmpty(message = "Seleccione al menos un interés (o 'Ninguno identificado')")
    private List<@NotNull Interes> gustos;

    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String gustosDetalle;

    @NotEmpty(message = "Seleccione al menos un rasgo de personalidad")
    private List<@NotNull RasgoPersonalidad> personalidad;

    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String personalidadDetalle;

    @NotEmpty(message = "Indique alergias conocidas (o 'Ninguna conocida')")
    private List<@NotNull Alergia> alergias;

    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String alergiasDetalle;

    @NotNull(message = "Indique si el niño o niña es intolerante a la lactosa")
    private RespuestaSiNoNoSabe intoleranciaLactosa;

    @NotEmpty(message = "Indique condiciones de salud conocidas (o 'Ninguna conocida')")
    private List<@NotNull CondicionSalud> condicionesSalud;

    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String condicionesSaludDetalle;

    @NotBlank(message = "Las necesidades de apoyo del niño o niña son obligatorias")
    @Size(max=2000, message="Utiliza hasta 2000 caracteres")
    private String necesidadesApoyo;

    @NotNull(message = "Indique si el niño o niña tiene hermanos")
    private SiNo tieneHermanos;

    @Min(value = 0, message = "La cantidad de hermanos no puede ser negativa")
    @JsonDeserialize(using=EnteroEstricto.class)
    private Integer cantidadHermanos;

    @Size(max=255, message="Utiliza hasta 255 caracteres")
    private String hermanosDetalle;

    /**
     * Campo opcional de continuidad de identidad/cultura (sección 2.7):
     * vínculos con una comunidad, idioma o cultura que conviene preservar.
     * Se mantiene como texto libre a propósito: convertirlo en una lista
     * cerrada lo transformaría en un filtro de características, algo que
     * el diseño del Sprint 0 descarta explícitamente (ver 2.6).
     */
    @Size(max=2000, message="Utiliza hasta 2000 caracteres")
    private String contextoCultural;

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

    @JsonIgnore @AssertTrue(message="Revisa las habilidades: selecciona opciones coherentes y describe «Otra»")
    public boolean isHabilidadesCoherentes() { return seleccionValida(habilidades, "NINGUNA_IDENTIFICADA", "OTRA", habilidadesDetalle); }
    @JsonIgnore @AssertTrue(message="Revisa los gustos: selecciona opciones coherentes y describe «Otro»")
    public boolean isGustosCoherentes() { return seleccionValida(gustos, "NINGUNO_IDENTIFICADO", "OTRO", gustosDetalle); }
    @JsonIgnore @AssertTrue(message="Describe la opción «Otro» de personalidad")
    public boolean isPersonalidadCoherente() { return seleccionValida(personalidad, null, "OTRO", personalidadDetalle); }
    @JsonIgnore @AssertTrue(message="Revisa las alergias: selecciona opciones coherentes y describe «Otra»")
    public boolean isAlergiasCoherentes() { return seleccionValida(alergias, "NINGUNA_CONOCIDA", "OTRA", alergiasDetalle)
            && (alergias == null || !alergias.contains(Alergia.ALIMENTARIA) || alergiasDetalle != null && !alergiasDetalle.isBlank()); }
    @JsonIgnore @AssertTrue(message="Revisa las condiciones de salud y describe «Otra» cuando corresponda")
    public boolean isCondicionesSaludCoherentes() { return seleccionValida(condicionesSalud, "NINGUNA_CONOCIDA", "OTRA", condicionesSaludDetalle); }
    @JsonIgnore @AssertTrue(message="Revisa la cantidad de hermanos: indica al menos uno si seleccionaste «Sí» y cero o vacío si seleccionaste «No»")
    public boolean isHermanosCoherentes() {
        return tieneHermanos == null || (tieneHermanos == SiNo.SI
                ? cantidadHermanos != null && cantidadHermanos > 0 : cantidadHermanos == null || cantidadHermanos == 0);
    }
    private boolean seleccionValida(List<? extends Enum<?>> opciones, String neutra, String otra, String detalle) {
        if (opciones == null || opciones.isEmpty()) return true; // @NotEmpty conserva el mensaje del campo.
        if (opciones.stream().anyMatch(java.util.Objects::isNull)) return false;
        if (neutra != null && opciones.stream().anyMatch(v -> neutra.equals(v.name())) && opciones.size() > 1) return false;
        return opciones.stream().noneMatch(v -> otra.equals(v.name())) || detalle != null && !detalle.isBlank();
    }

    /** Evita truncar silenciosamente edades o cantidades decimales recibidas por JSON. */
    public static class EnteroEstricto extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext contexto) throws java.io.IOException {
            if (!parser.hasToken(com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT))
                return (Integer) contexto.handleUnexpectedToken(Integer.class, parser);
            return parser.getIntValue();
        }
    }
}
