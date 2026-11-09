package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Reúne actuaciones existentes después del control de acceso; no escribe eventos ni reconstruye datos históricos ausentes. */
@Service
public class HistorialCasoService {
    private final EvaluacionPsicologicaRepository evaluaciones;
    private final SolicitudEstadoCasoRepository solicitudes;
    private final DecisionComiteRepository decisiones;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
    public HistorialCasoService(EvaluacionPsicologicaRepository e,SolicitudEstadoCasoRepository s,DecisionComiteRepository d) {
        evaluaciones=e;solicitudes=s;decisiones=d;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> consultar(String codigo,List<EventoCaso> auditoria) {
        var informes=evaluaciones.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(codigo);
        var tramites=solicitudes.findByExpedienteCodigoCasoOrderByFechaSolicitudDescIdDesc(codigo);
        Set<Long> idsInformes=new HashSet<>(),idsSolicitudes=new HashSet<>(),revisados=new HashSet<>();
        informes.forEach(e->{idsInformes.add(e.getId());if(e.getRevisionDe()!=null)revisados.add(e.getRevisionDe().getId());});
        tramites.forEach(s->idsSolicitudes.add(s.getId()));
        List<Map<String,Object>> lista=new ArrayList<>();
        for(EventoCaso e:auditoria) {
            // Solo se agrupan duplicados que contienen una referencia explícita a un registro existente.
            Long referencia=referencia(e.getDescripcion());
            if(referencia!=null && ((Set.of("EVALUACION_PSICOLOGICA","CORRECCION_PSICOLOGICA").contains(e.getAccion()) && idsInformes.contains(referencia))
                    || ((e.getAccion().startsWith("SOLICITUD_") || e.getAccion().matches("(DESACTIVACION|REACTIVACION)_(APROBADA|RECHAZADA)")) && idsSolicitudes.contains(referencia)))) continue;
            Map<String,Object> detalle=detalle(e.getDescripcion());
            var evento=evento("auditoria:"+e.getId(),e.getAccion(),e.getActor(),detalle.get("rol") instanceof String r?r:null,e.getFecha());
            evento.put("descripcion",detalle.getOrDefault("resumen",resumen(e.getAccion())));
            evento.put("detalles",detalle);lista.add(evento);
        }
        for(var e:informes) {
            var v=evento("evaluacion:"+e.getId(),e.getRevisionDe()==null?"EVALUACION_PSICOLOGICA":"CORRECCION_PSICOLOGICA",e.getProfesional(),"PSICOLOGO",e.getFecha());
            v.put("descripcion",e.getRevisionDe()==null?"Se registró una evaluación psicológica.":"Se registró una nueva versión, conservando el informe anterior.");
            v.put("evaluacionId",e.getId());v.put("version",e.getNumeroVersion()==null?1:e.getNumeroVersion());v.put("vigente",!revisados.contains(e.getId()));lista.add(v);
        }
        for(var s:tramites) {
            var v=evento("solicitud:"+s.getId(),"SOLICITUD_"+s.getTipo(),s.getSolicitante(),"TRABAJADOR_SOCIAL",s.getFechaSolicitud());
            v.put("descripcion","Se envió una solicitud al Comité.");v.put("detalles",Map.of("justificacion",s.getMotivo(),"tipo",s.getTipo().name()));lista.add(v);
            if(s.getFechaResolucion()!=null && s.getResueltoPor()!=null) {
                var r=evento("resolucion:"+s.getId(),s.getTipo()+"_"+s.getEstado(),s.getResueltoPor(),"COMITE",s.getFechaResolucion());
                r.put("descripcion",s.getEstado()==SolicitudEstadoCaso.Estado.APROBADA?"El Comité aprobó la solicitud.":"El Comité rechazó la solicitud.");
                r.put("detalles",Map.of("justificacion",s.getMotivoResolucion()==null?"":s.getMotivoResolucion()));lista.add(r);
            }
        }
        for(var d:decisiones.findByCodigoNinoOrderByFechaAsc(codigo)) {
            var v=evento("decision:"+d.getId(),"DECISION_"+d.getDecision(),d.getFuncionario(),"COMITE",d.getFecha());
            v.put("descripcion","Se registró una decisión sobre una propuesta de vinculación.");
            v.put("detalles",Map.of("justificacion",d.getMotivo(),"familia",d.getCodigoFamilia(),"puntaje",d.getPuntaje(),"versionPesos",d.getVersionPesos()));lista.add(v);
        }
        lista.sort(Comparator.comparing((Map<String,Object> m)->(OffsetDateTime)m.get("fecha"),Comparator.nullsLast(Comparator.reverseOrder())));return lista;
    }
    private Map<String,Object> evento(String clave,String accion,String actor,String rol,LocalDateTime fecha) {
        Map<String,Object> m=new LinkedHashMap<>();m.put("clave",clave);m.put("accion",accion);m.put("actor",actor);m.put("rol",rol);
        m.put("fecha",fecha==null?null:fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime());return m;
    }
    private Long referencia(String descripcion) {
        if(descripcion==null)return null;
        var d=detalle(descripcion);Object valor=d.get("referencia");if(valor instanceof Number n)return n.longValue();
        Matcher m=Pattern.compile("(?:Evaluación registrada: |revisión #|Solicitud #|solicitud #)(\\d+)").matcher(descripcion);
        Long ultimo=null;while(m.find())ultimo=Long.valueOf(m.group(1));return ultimo;
    }
    private Map<String,Object> detalle(String descripcion) {
        if(descripcion==null || !descripcion.startsWith("{"))return Map.of();
        try {return json.readValue(descripcion,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}
        catch(Exception ignorado){return Map.of();}
    }
    private String resumen(String accion) {
        return switch(accion) {
            case "CREACION" -> "Se creó el perfil infantil.";
            case "EDICION" -> "Se actualizó la información social. Este registro anterior no conserva el detalle de los campos modificados.";
            default -> "Actuación conservada en el expediente. No hay un detalle ampliado registrado.";
        };
    }
}
