package com.senniaf.match.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record EvaluacionPsicologicaRequest(
 @NotBlank @Size(max=5000) String evaluacion,
 @NotBlank @Size(max=5000) String observaciones,
 @NotBlank @Size(max=5000) String recomendaciones,
 @Size(max=2000) String motivoCorreccion,
 Boolean sinDiagnostico,
 @Size(max=6) List<@Pattern(regexp="ADAPTACION_EMOCIONAL|INTERACCION_SOCIAL|CONDUCTA|APRENDIZAJE|RELACIONES_AFECTIVAS|ACOMPANAMIENTO") String> areasObservadas,
 @Size(max=5000) String hallazgosProfesionales,
 @Size(max=5000) String diagnosticoProfesional,
 @jakarta.validation.Valid ContextoEvaluacion contextoEvaluacion) {
 public EvaluacionPsicologicaRequest(String e,String o,String r,String m,Boolean sin,List<String> areas,String hallazgos,String diagnostico){this(e,o,r,m,sin,areas,hallazgos,diagnostico,null);}
 public EvaluacionPsicologicaRequest(String e,String o,String r,String m){this(e,o,r,m,null,null,null,null,null);}
}
