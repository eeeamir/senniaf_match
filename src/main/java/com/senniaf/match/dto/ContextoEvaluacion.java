package com.senniaf.match.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.HashSet;

/** Contexto profesional de cada versión; nunca interviene en la puntuación del Match. */
public record ContextoEvaluacion(
        @Min(1) @Max(1) int formato,
        @Size(max=5) List<@NotNull @Pattern(regexp="ENTREVISTA_INDIVIDUAL|OBSERVACION_DIRECTA|REVISION_ANTECEDENTES|INSTRUMENTOS|OTRO") String> metodos,
        @Size(max=2000) String otroMetodo,
        @Size(max=2000) String instrumentos,
        @Size(max=5000) String resultadosInstrumentos,
        @Size(max=6) List<@NotNull @Pattern(regexp="EMOCIONAL|EDUCATIVO|RUTINAS|VINCULOS|SEGUIMIENTO_PSICOLOGICO|OTRO") String> apoyos,
        @Size(max=2000) String otroApoyo,
        @Size(max=5000) String detalleAcompanamiento) {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static boolean texto(String s){return s!=null&&!s.isBlank();}
    private static boolean contiene(List<String> valores,String valor){return valores!=null&&valores.contains(valor);}
    @JsonIgnore @AssertTrue(message="Especifica el otro método únicamente cuando lo selecciones")
    public boolean isOtroMetodoCoherente(){return contiene(metodos,"OTRO")==texto(otroMetodo);}
    @JsonIgnore @AssertTrue(message="Al utilizar instrumentos, identifica cuáles y sus resultados pertinentes")
    public boolean isInstrumentosCoherentes(){return contiene(metodos,"INSTRUMENTOS")?(texto(instrumentos)&&texto(resultadosInstrumentos)):!texto(instrumentos)&&!texto(resultadosInstrumentos);}
    @JsonIgnore @AssertTrue(message="Especifica el otro apoyo únicamente cuando lo selecciones")
    public boolean isOtroApoyoCoherente(){return contiene(apoyos,"OTRO")==texto(otroApoyo);}
    @JsonIgnore @AssertTrue(message="Describe la necesidad y su contexto cuando selecciones apoyos")
    public boolean isDetalleApoyosCoherente(){return apoyos==null||apoyos.isEmpty()||texto(detalleAcompanamiento);}
    @JsonIgnore @AssertTrue(message="No repitas métodos ni apoyos")
    public boolean isSeleccionUnica(){return (metodos==null||new HashSet<>(metodos).size()==metodos.size())&&(apoyos==null||new HashSet<>(apoyos).size()==apoyos.size());}
    // El formato acompaña cada versión del informe; NULL identifica antecedentes sin contexto registrado.
    public String serializar(){
        try{return JSON.writeValueAsString(this);}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("No se pudo conservar el contexto de la evaluación",e);}
    }
    public static ContextoEvaluacion leer(String valor){
        if(valor==null)return null;
        try{return JSON.readValue(valor,ContextoEvaluacion.class);}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("No se pudo consultar el contexto registrado",e);}
    }
}
