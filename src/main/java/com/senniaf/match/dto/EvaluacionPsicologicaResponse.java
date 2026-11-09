package com.senniaf.match.dto;

import com.senniaf.match.model.EvaluacionPsicologica;
import java.time.LocalDateTime;

/** No serializa relaciones JPA ni datos de credenciales del profesional. */
public record EvaluacionPsicologicaResponse(Long id, String codigoCaso, String profesional,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=com.senniaf.match.web.FechaConZonaSerializer.class) LocalDateTime fecha, String evaluacion, String observaciones, String recomendaciones, Long revisionDe, int version, boolean vigente, String motivoCorreccion, Boolean sinDiagnostico, java.util.List<String> areasObservadas, String hallazgosProfesionales, String diagnosticoProfesional, ContextoEvaluacion contextoEvaluacion) {
    private static java.util.List<String> areas(String texto) {
        if(texto==null)return java.util.List.of();
        try{return new com.fasterxml.jackson.databind.ObjectMapper().readValue(texto,new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>(){});}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("No se pudo leer la valoración registrada",e);}
    }
    public static EvaluacionPsicologicaResponse de(EvaluacionPsicologica e, boolean vigente) {
        return new EvaluacionPsicologicaResponse(e.getId(), e.getExpediente().getCodigoCaso(),
                e.getProfesional(), e.getFecha(), e.getEvaluacion(), e.getObservaciones(), e.getRecomendaciones(),
                e.getRevisionDe()==null ? null : e.getRevisionDe().getId(),
                e.getNumeroVersion()==null ? 1 : e.getNumeroVersion(),vigente,e.getMotivoCorreccion(),e.getSinDiagnostico(),areas(e.getAreasObservadas()),e.getHallazgosProfesionales(),e.getDiagnosticoProfesional(),ContextoEvaluacion.leer(e.getContextoEvaluacion()));
    }
}
