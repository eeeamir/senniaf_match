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
    @org.springframework.beans.factory.annotation.Autowired private CombinacionExcluidaRepository excluidas;
    private final VinculacionRepository repo;
    private final EventoCasoRepository auditoria;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
    private final InterrupcionConvivenciaRepository interrupciones;
    public VinculacionService(VinculacionRepository repo,EventoCasoRepository auditoria){this(repo,auditoria,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public VinculacionService(VinculacionRepository repo,EventoCasoRepository auditoria,InterrupcionConvivenciaRepository interrupciones){this.repo=repo;this.auditoria=auditoria;this.interrupciones=interrupciones;}

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
            .filter(v->estado==null || estado.isBlank() || estadoVisible(v).name().equals(estado))
            .filter(v->buscar==null || ("VIN-"+v.getId()+" "+v.getNino().getCodigoCaso()+" "+v.getFamilia().getCodigoCaso()).toLowerCase(Locale.ROOT).contains(buscar.toLowerCase(Locale.ROOT)))
            .map(v->resumen(v)).toList();
    }
    public Map<String,Object> detalle(Long id,String rol,String actor){
        institucional(rol);
        Vinculacion v=repo.findById(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(!autorizado(v,rol,actor))throw new RolNoAutorizadoException("No tienes acceso a esta vinculación");
        var r=vista(v,false);
        var interrupcion=interrupcion(v);
        r.put("compatibilidadOriginal",compatibilidadOriginal(v.getDecision()));
        r.put("interrupcion",interrupcion==null?null:detalleInterrupcion(interrupcion));
        r.put("puedeCerrar","COMITE".equals(rol) && v.getEstado()!=Vinculacion.Estado.CERRADA && !rechazoPortal(v));
        r.put("puedeRevisarDisponibilidad","COMITE".equals(rol) && v.getEstado()==Vinculacion.Estado.CERRADA);
        r.put("cierre",v.getFechaCierre()==null?null:datosCierre(v));
        r.put("puedeReportarInterrupcion",!"COMITE".equals(rol) && interrupcion==null && reportable(v));
        r.put("puedeRevisarInterrupcion","COMITE".equals(rol) && interrupcion!=null && interrupcion.getFechaRevision()==null);
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
        if("RECHAZAR".equals(respuesta)){
            if(excluidas!=null && !excluidas.existsByCodigoNinoAndCodigoFamilia(v.getNino().getCodigoCaso(),v.getFamilia().getCodigoCaso())){
                var e=new CombinacionExcluida();e.codigoNino=v.getNino().getCodigoCaso();e.codigoFamilia=v.getFamilia().getCodigoCaso();e.origen="FAMILIA";e.responsable=actor;e.fecha=LocalDateTime.now();excluidas.save(e);
            }
            if(interrupcion(v)==null && v.getFechaCierre()==null)v.setReservaVigente(false);
        }
        v.setEstado("ACEPTAR".equals(respuesta)?Vinculacion.Estado.RESPUESTA_ACEPTADA:Vinculacion.Estado.RECHAZADA_POR_FAMILIA);
        v.setFechaRespuesta(LocalDateTime.now());v.setRespondidoPor(actor);repo.save(v);
        auditar(v,actor,"RESPUESTA_FAMILIAR",v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA?"La familia aceptó la propuesta.":"La familia rechazó la propuesta del portal. La combinación no se recomendará automáticamente; se mantienen las condiciones vigentes de cada expediente.");
        return vista(v,true);
    }
    private void institucional(String rol){if(!Set.of("COMITE","TRABAJADOR_SOCIAL","PSICOLOGO").contains(rol))throw new RolNoAutorizadoException("Solo el equipo institucional autorizado puede consultar vinculaciones");}
    private boolean autorizado(Vinculacion v,String rol,String actor){return "COMITE".equals(rol) || "TRABAJADOR_SOCIAL".equals(rol)&&actor.equals(v.getNino().getTrabajadorSocialAsignado()) || "PSICOLOGO".equals(rol)&&actor.equals(v.getNino().getPsicologoAsignado());}
    private Map<String,Object> resumen(Vinculacion v){
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",v.getId());r.put("codigo","VIN-"+v.getId());r.put("expedienteInfantil",v.getNino().getCodigoCaso());r.put("expedienteFamiliar",v.getFamilia().getCodigoCaso());r.put("fechaCreacion",fecha(v.getDecision().getFecha()));r.put("estado",estadoVisible(v));r.put("puntajeOriginal",v.getDecision().getPuntaje());r.put("versionOriginal",v.getDecision().getVersionPesos());var i=interrupcion(v);r.put("interrupcionPendiente",i!=null && i.getFechaRevision()==null);r.put("interrupcionRegistrada",i!=null);return r;
    }
    private Map<String,Object> vista(Vinculacion v,boolean familiar){
        var r=resumen(v);
        // No hay una política documentada que autorice datos infantiles en el portal familiar.
        if(familiar){r.remove("expedienteInfantil");r.remove("puntajeOriginal");r.remove("versionOriginal");}
        r.put("fechaRespuesta",fecha(v.getFechaRespuesta()));r.put("disponibilidad",!v.isReservaVigente()?"Pendiente de confirmar elegibilidad para Match":v.getEstado()==Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE?"Reservado; pendiente de confirmación institucional":"Reservado por asignación vigente");
        r.put("siguientePaso",switch(v.getEstado()){
            case RECHAZADA_POR_FAMILIA -> "Tu respuesta quedó registrada. Gracias por revisar la propuesta. El equipo correspondiente podrá continuar con el seguimiento de tu expediente.";
            case CERRADA -> "La vinculación está cerrada. La disponibilidad de cada expediente requiere revisión institucional independiente.";
            case PENDIENTE_RESPUESTA -> "Esta vinculación está pendiente de respuesta familiar.";
            case RESPUESTA_ACEPTADA -> "La familia aceptó la propuesta. El proceso continuará conforme a las actuaciones institucionales correspondientes.";
            case RESPUESTA_RECHAZADA_PENDIENTE_CIERRE -> "La familia rechazó la propuesta. El equipo debe confirmar el cierre y la disponibilidad de los expedientes.";
        });
        if(familiar)r.put("siguientePaso",switch(v.getEstado()){
            case RECHAZADA_POR_FAMILIA -> "Tu respuesta quedó registrada. Gracias por revisar la propuesta. El equipo correspondiente podrá continuar con el seguimiento de tu expediente.";
            case CERRADA -> "La vinculación está cerrada. La disponibilidad de cada expediente requiere revisión institucional independiente.";
            case PENDIENTE_RESPUESTA -> "Tienes una propuesta pendiente. Revisa la información disponible antes de responder.";
            case RESPUESTA_ACEPTADA -> "Aceptaste la propuesta. El equipo correspondiente continuará con el proceso.";
            case RESPUESTA_RECHAZADA_PENDIENTE_CIERRE -> "Rechazaste la propuesta. Tu respuesta quedó registrada. El equipo revisará el cierre institucional.";
        });
        if(v.getEstado()==Vinculacion.Estado.CERRADA)r.put("disponibilidad","Cada expediente requiere revisión independiente; el cierre no libera automáticamente las reservas.");
        var seguimiento=interrupcion(v);
        if(seguimiento!=null && v.getEstado()!=Vinculacion.Estado.CERRADA)r.put("siguientePaso",familiar
                ?"El equipo institucional te orientará sobre los siguientes pasos de esta vinculación."
                :seguimiento.getFechaRevision()==null?"La interrupción de convivencia está pendiente de revisión del Comité.":"El Comité registró su revisión. Los expedientes conservan su reserva hasta la actuación institucional correspondiente.");
        if(rechazoPortal(v)){
            r.put("siguientePaso",familiar?"Tu respuesta quedó registrada. Gracias por revisar la propuesta. El equipo correspondiente podrá continuar con el seguimiento de tu expediente.":"La propuesta del portal fue rechazada. Esta combinación queda fuera de las recomendaciones automáticas, sin cambiar la idoneidad de los expedientes.");
            r.put("disponibilidad","Sin reserva por esta propuesta; se mantienen los requisitos vigentes de cada expediente.");
        }
        if(!familiar){r.put("decisionComite",Map.of("decision",v.getDecision().getDecision(),"responsable",v.getDecision().getFuncionario(),"fecha",fecha(v.getDecision().getFecha()),"motivo",v.getDecision().getMotivo()));r.put("respondidoPor",v.getRespondidoPor());}
        return r;
    }
    @Transactional
    public Map<String,Object> registrarInterrupcion(Long id,String rol,String actor,com.senniaf.match.dto.InterrupcionRequest solicitud){
        if(!Set.of("TRABAJADOR_SOCIAL","PSICOLOGO").contains(rol))throw new RolNoAutorizadoException("Solo el profesional asignado puede registrar la interrupción");
        Vinculacion v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(!autorizado(v,rol,actor))throw new RolNoAutorizadoException("No tienes asignada esta vinculación");
        if(!reportable(v))throw new IllegalStateException("Esta vinculación no permite registrar una interrupción de convivencia");
        if(interrupcion(v)!=null)throw new IllegalStateException("Esta vinculación ya tiene una interrupción registrada. Consulta su revisión");
        LocalDateTime ahora=LocalDateTime.now();
        if(solicitud==null || solicitud.fechaInterrupcion()==null || solicitud.fechaInterrupcion().isAfter(java.time.LocalDate.now(ZoneId.of("America/Panama")))
                || solicitud.fechaInterrupcion().isBefore(fechaPanama(v.getFechaRespuesta())))
            throw new IllegalArgumentException("La fecha debe estar entre la aceptación familiar y el día de hoy");
        InterrupcionConvivencia i=new InterrupcionConvivencia();i.setVinculacion(v);i.setFechaInterrupcion(solicitud.fechaInterrupcion());
        i.setReferenciaActuacion(texto(solicitud.referenciaActuacion(),5,200,"Indica la referencia institucional de la convivencia"));
        i.setJustificacion(texto(solicitud.justificacion(),10,2000,"Explica el motivo de la interrupción en 10 a 2000 caracteres"));
        i.setRegistradoPor(actor);i.setRolProfesional(rol);i.setFechaRegistro(ahora);interrupciones.saveAndFlush(i);
        auditar(v,actor,"INTERRUPCION_REGISTRADA","Se registró una interrupción de convivencia para revisión del Comité.");
        return detalle(id,rol,actor);
    }
    @Transactional
    public Map<String,Object> revisarInterrupcion(Long id,String rol,String actor,com.senniaf.match.dto.RevisionInterrupcionRequest solicitud){
        if(!"COMITE".equals(rol))throw new RolNoAutorizadoException("Solo el Comité puede registrar esta revisión");
        Vinculacion v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        var i=interrupcion(v);
        if(i==null)throw new IllegalStateException("Esta vinculación no tiene una interrupción para revisar");
        if(i.getFechaRevision()!=null)throw new IllegalStateException("La revisión ya está registrada y se conserva en el historial");
        i.setRevisionJustificacion(texto(solicitud==null?null:solicitud.justificacion(),10,2000,"Explica la revisión realizada en 10 a 2000 caracteres"));
        i.setRevisadoPor(actor);i.setFechaRevision(LocalDateTime.now());interrupciones.saveAndFlush(i);
        auditar(v,actor,"INTERRUPCION_REVISADA","El Comité registró su revisión de la interrupción. Se conserva la reserva hasta la actuación institucional correspondiente.");
        return detalle(id,rol,actor);
    }
    @Transactional
    public Map<String,Object> cerrar(Long id,String rol,String actor,com.senniaf.match.dto.CierreInstitucionalRequest solicitud){
        exigirComite(rol);
        var v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(rechazoPortal(v))throw new IllegalStateException("La propuesta del portal ya fue rechazada y no requiere un cierre institucional");
        if(v.getEstado()==Vinculacion.Estado.CERRADA)throw new IllegalStateException("El cierre ya está registrado y se conserva en el historial");
        if(solicitud==null || !solicitud.confirmacionAutoridad())throw new IllegalArgumentException("Confirma la actuación de la autoridad competente");
        var ahora=LocalDateTime.now();
        v.setMotivoCierre(texto(solicitud.motivo(),10,2000,"Explica el motivo o resultado del cierre"));
        v.setFechaCierre(fechaPanama(ahora));v.setCerradoPor(actor);v.setFechaRegistroCierre(ahora);v.setEstado(Vinculacion.Estado.CERRADA);repo.save(v);
        auditar(v,actor,"CIERRE_INSTITUCIONAL","El Comité registró el cierre con respaldo institucional confirmado. Cada expediente conserva su reserva hasta una revisión independiente.");
        return detalle(id,rol,actor);
    }
    @Transactional
    public Map<String,Object> revisarDisponibilidad(Long id,String rol,String actor,String expediente,String justificacion){
        exigirComite(rol);
        if(!Set.of("NINO","FAMILIA").contains(expediente==null?"":expediente))throw new IllegalArgumentException("Selecciona el expediente que se ha revisado");
        var v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(v.getEstado()!=Vinculacion.Estado.CERRADA)throw new IllegalStateException("Primero debe registrarse el cierre institucional");
        String motivo=texto(justificacion,10,600,"Documenta la revisión de disponibilidad y elegibilidad");
        if("NINO".equals(expediente)){
            if(v.isNinoLiberado())throw new IllegalStateException("La revisión de este expediente ya fue registrada");
            if(!v.getNino().isPerfilCompleto() || v.getNino().isBajaLogica() || v.getNino().isCierrePorAdopcion())throw new IllegalStateException("El expediente infantil no reúne las condiciones vigentes para volver al Match");
            v.setNinoLiberado(true);
        }else{
            if(v.isFamiliaLiberada())throw new IllegalStateException("La revisión de este expediente ya fue registrada");
            if(!v.getFamilia().isPerfilCompleto() || v.getFamilia().isReevaluacionPendiente())throw new IllegalStateException("El expediente familiar no reúne las condiciones vigentes para volver al Match");
            v.setFamiliaLiberada(true);
        }
        if(v.isNinoLiberado() && v.isFamiliaLiberada())v.setReservaVigente(false);
        repo.save(v);
        // Los datos de esta revisión no modifican el informe ni la decisión original.
        EventoCaso e=new EventoCaso();e.setCodigoCaso(v.getNino().getCodigoCaso());e.setActor(actor);e.setFecha(LocalDateTime.now());e.setAccion("DISPONIBILIDAD_REVISADA");
        try{e.setDescripcion(json.writeValueAsString(Map.of("vinculacionId",v.getId(),"expediente",expediente,"justificacion",motivo,"resumen","El Comité confirmó por separado la disponibilidad y elegibilidad del expediente "+("NINO".equals(expediente)?"infantil":"familiar")+" para Match.")));}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException("No se pudo conservar la revisión",error);}if(e.getDescripcion().length()>1000)throw new IllegalArgumentException("Reduce la justificación para conservarla completa en el historial");auditoria.save(e);
        return detalle(id,rol,actor);
    }
    private void exigirComite(String rol){if(!"COMITE".equals(rol))throw new RolNoAutorizadoException("Solo el Comité puede registrar esta actuación institucional");}
    private Map<String,Object> datosCierre(Vinculacion v){
        var r=new LinkedHashMap<String,Object>();r.put("fecha",v.getFechaCierre());r.put("referencia",v.getReferenciaCierre());r.put("autoridad",v.getAutoridadCierre());r.put("motivo",v.getMotivoCierre());r.put("registradoPor",v.getCerradoPor());r.put("registradoEl",fecha(v.getFechaRegistroCierre()));r.put("ninoLiberado",v.isNinoLiberado());r.put("familiaLiberada",v.isFamiliaLiberada());return r;
    }
    /** Solo lee el fundamento guardado; nunca invoca el cálculo actual. */
    private Map<String,Object> compatibilidadOriginal(DecisionComite d){
        var r=new LinkedHashMap<String,Object>();r.put("puntaje",d.getPuntaje());r.put("version",d.getVersionPesos());
        try{var c=d.getContextoScoring()==null?null:json.readTree(d.getContextoScoring());r.put("contexto",c!=null&&c.isObject()?c:null);}catch(com.fasterxml.jackson.core.JsonProcessingException error){r.put("contexto",null);}return r;
    }
    private boolean rechazoPortal(Vinculacion v){
        return v.getEstado()==Vinculacion.Estado.RECHAZADA_POR_FAMILIA || v.getEstado()==Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE && !v.isReservaVigente() && v.getFechaCierre()==null && interrupcion(v)==null;
    }
    private Vinculacion.Estado estadoVisible(Vinculacion v){return rechazoPortal(v)?Vinculacion.Estado.RECHAZADA_POR_FAMILIA:v.getEstado();}
    private boolean reportable(Vinculacion v){return v.isReservaVigente() && v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA && v.getFechaRespuesta()!=null && !v.getNino().isCierrePorAdopcion();}
    private InterrupcionConvivencia interrupcion(Vinculacion v){return interrupciones==null?null:interrupciones.findByVinculacionId(v.getId()).orElse(null);}
    private Map<String,Object> detalleInterrupcion(InterrupcionConvivencia i){
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",i.getId());r.put("fechaInterrupcion",i.getFechaInterrupcion());
        r.put("referenciaActuacion",i.getReferenciaActuacion());r.put("justificacion",i.getJustificacion());r.put("registradoPor",i.getRegistradoPor());r.put("rolProfesional",i.getRolProfesional());r.put("fechaRegistro",fecha(i.getFechaRegistro()));
        r.put("revisionJustificacion",i.getRevisionJustificacion());r.put("revisadoPor",i.getRevisadoPor());r.put("fechaRevision",fecha(i.getFechaRevision()));return r;
    }
    private String texto(String valor,int min,int max,String error){if(valor==null || valor.strip().length()<min || valor.length()>max)throw new IllegalArgumentException(error);return valor.strip();}
    private java.time.LocalDate fechaPanama(LocalDateTime valor){return valor.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("America/Panama")).toLocalDate();}
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
