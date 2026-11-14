package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class VinculacionService {
    private final VinculacionRepository repo;
    private final EventoCasoRepository auditoria;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
    public VinculacionService(VinculacionRepository repo,EventoCasoRepository auditoria){this.repo=repo;this.auditoria=auditoria;}

    @Transactional
    public void crear(DecisionComite decision,PerfilNino nino,PerfilFamilia familia){
        if(repo.existsByNinoCodigoCasoAndReservaVigenteTrue(nino.getCodigoCaso())
                || repo.existsByFamiliaCodigoCasoAndReservaVigenteTrue(familia.getCodigoCaso()))
            throw new IllegalStateException("No puedes continuar porque el expediente ya tiene una asignación vigente");
        Vinculacion v=new Vinculacion();v.setDecision(decision);v.setNino(nino);v.setFamilia(familia);
        repo.saveAndFlush(v);
        auditar(v,decision.getFuncionario(),"VINCULACION_CREADA","El Comité aceptó la propuesta. Se creó la vinculación pendiente de respuesta familiar.");
    }
    public List<Map<String,Object>> propias(String actor){
        return repo.findByFamiliaRegistradoPorOrderByIdDesc(actor).stream().map(v->vista(v,true)).toList();
    }
    public List<Map<String,Object>> listar(String rol,String actor,String estado,String buscar){
        institucional(rol);
        return repo.findAllByOrderByIdDesc().stream().filter(v->autorizado(v,rol,actor))
            .filter(v->estado==null || estado.isBlank() || v.getEstado().name().equals(estado))
            .filter(v->buscar==null || ("VIN-"+v.getId()+" "+v.getNino().getCodigoCaso()+" "+v.getFamilia().getCodigoCaso()).toLowerCase(Locale.ROOT).contains(buscar.toLowerCase(Locale.ROOT)))
            .map(v->resumen(v)).toList();
    }
    public Map<String,Object> detalle(Long id,String rol,String actor){
        institucional(rol);
        Vinculacion v=repo.findById(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(!autorizado(v,rol,actor))throw new RolNoAutorizadoException("No tienes acceso a esta vinculación");
        var r=vista(v,false);
        // La misma auditoría conserva el historial infantil y el detalle de la vinculación.
        r.put("historial",auditoria.findByCodigoCasoOrderByFechaAsc(v.getNino().getCodigoCaso()).stream()
            .filter(e->detalleEvento(e).path("vinculacionId").asLong(-1)==v.getId())
            .map(e->{
                Map<String,Object> evento=new LinkedHashMap<>();
                evento.put("accion",e.getAccion());evento.put("responsable",e.getActor());
                evento.put("fecha",fecha(e.getFecha()));
                evento.put("resumen",detalleEvento(e).path("resumen").asText("Actuación registrada"));
                return evento;
            }).toList());
        return r;
    }
    @Transactional
    public Map<String,Object> responder(Long id,String actor,String respuesta){
        if(!Set.of("ACEPTAR","RECHAZAR").contains(respuesta==null?"":respuesta))throw new IllegalArgumentException("Selecciona aceptar o rechazar la propuesta");
        Vinculacion v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(!actor.equals(v.getFamilia().getRegistradoPor()))throw new RolNoAutorizadoException("No tienes acceso a esta vinculación");
        if(!v.isReservaVigente() || v.getEstado()!=Vinculacion.Estado.PENDIENTE_RESPUESTA)
            throw new IllegalStateException("Esta propuesta ya tiene una respuesta registrada o dejó de estar vigente");
        v.setEstado("ACEPTAR".equals(respuesta)?Vinculacion.Estado.RESPUESTA_ACEPTADA:Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE);
        v.setFechaRespuesta(LocalDateTime.now());v.setRespondidoPor(actor);repo.save(v);
        auditar(v,actor,"RESPUESTA_FAMILIAR",v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA?"La familia aceptó la propuesta.":"La familia rechazó la propuesta. Pendiente de cierre institucional.");
        return vista(v,true);
    }
    private void institucional(String rol){if(!Set.of("COMITE","TRABAJADOR_SOCIAL","PSICOLOGO").contains(rol))throw new RolNoAutorizadoException("Solo el equipo institucional autorizado puede consultar vinculaciones");}
    private boolean autorizado(Vinculacion v,String rol,String actor){return "COMITE".equals(rol) || "TRABAJADOR_SOCIAL".equals(rol)&&actor.equals(v.getNino().getTrabajadorSocialAsignado()) || "PSICOLOGO".equals(rol)&&actor.equals(v.getNino().getPsicologoAsignado());}
    private Map<String,Object> resumen(Vinculacion v){
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",v.getId());r.put("codigo","VIN-"+v.getId());r.put("expedienteInfantil",v.getNino().getCodigoCaso());r.put("expedienteFamiliar",v.getFamilia().getCodigoCaso());r.put("fechaCreacion",fecha(v.getDecision().getFecha()));r.put("estado",v.getEstado());return r;
    }
    private Map<String,Object> vista(Vinculacion v,boolean familiar){
        var r=resumen(v);
        // No hay una política documentada que autorice datos infantiles en el portal familiar.
        if(familiar)r.remove("expedienteInfantil");
        r.put("fechaRespuesta",fecha(v.getFechaRespuesta()));r.put("disponibilidad",!v.isReservaVigente()?"Pendiente de confirmar elegibilidad para Match":v.getEstado()==Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE?"Reservado; pendiente de confirmación institucional":"Reservado por asignación vigente");
        r.put("siguientePaso",switch(v.getEstado()){
            case PENDIENTE_RESPUESTA -> "Esta vinculación está pendiente de respuesta familiar.";
            case RESPUESTA_ACEPTADA -> "La familia aceptó la propuesta. El proceso continuará conforme a las actuaciones institucionales correspondientes.";
            case RESPUESTA_RECHAZADA_PENDIENTE_CIERRE -> "La familia rechazó la propuesta. El equipo debe confirmar el cierre y la disponibilidad de los expedientes.";
        });
        if(familiar)r.put("siguientePaso",switch(v.getEstado()){
            case PENDIENTE_RESPUESTA -> "Tienes una propuesta pendiente. Revisa la información disponible antes de responder.";
            case RESPUESTA_ACEPTADA -> "Aceptaste la propuesta. El equipo correspondiente continuará con el proceso.";
            case RESPUESTA_RECHAZADA_PENDIENTE_CIERRE -> "Rechazaste la propuesta. Tu respuesta quedó registrada. El equipo revisará el cierre institucional.";
        });
        if(!familiar){r.put("decisionComite",Map.of("decision",v.getDecision().getDecision(),"responsable",v.getDecision().getFuncionario(),"fecha",fecha(v.getDecision().getFecha()),"motivo",v.getDecision().getMotivo()));r.put("respondidoPor",v.getRespondidoPor());}
        return r;
    }
    private Object fecha(LocalDateTime valor){return valor==null?null:valor.atZone(ZoneId.systemDefault()).toOffsetDateTime();}
    private com.fasterxml.jackson.databind.JsonNode detalleEvento(EventoCaso evento){
        try{
            var detalle=evento.getDescripcion()==null?null:json.readTree(evento.getDescripcion());
            return detalle!=null && detalle.isObject()?detalle:json.createObjectNode();
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException error){return json.createObjectNode();}
    }
    private void auditar(Vinculacion v,String actor,String accion,String resumen){
        EventoCaso e=new EventoCaso();e.setCodigoCaso(v.getNino().getCodigoCaso());e.setActor(actor);e.setAccion(accion);e.setFecha(LocalDateTime.now());
        e.setDescripcion("{\"vinculacionId\":"+v.getId()+",\"estado\":\""+v.getEstado()+"\",\"resumen\":\""+resumen+"\"}");auditoria.save(e);
    }
}
