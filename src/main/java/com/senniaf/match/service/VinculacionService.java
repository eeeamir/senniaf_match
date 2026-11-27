package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.model.Vinculacion.ResultadoCierre;
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
    @org.springframework.beans.factory.annotation.Autowired private PerfilNinoRepository ninos;
    @org.springframework.beans.factory.annotation.Autowired private PerfilFamiliaRepository familias;
    @org.springframework.beans.factory.annotation.Autowired private SolicitudEstadoCasoRepository solicitudesInfantiles;
    @org.springframework.beans.factory.annotation.Autowired private ReincorporacionService reincorporaciones;
    @org.springframework.beans.factory.annotation.Autowired private IncidenciaAsignacionRepository incidenciasAsignacion;
    @org.springframework.beans.factory.annotation.Autowired private VisitaDomiciliariaRepository visitasDomiciliarias;
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
    private boolean accesoInfantilFamiliar(Vinculacion v){
        return v.isReservaVigente() && Set.of(Vinculacion.Estado.PENDIENTE_RESPUESTA,Vinculacion.Estado.RESPUESTA_ACEPTADA).contains(v.getEstado()) && interrupcion(v)==null;
    }
    /** Proyección cerrada: datos estructurados de la asignación, nunca entidades ni informes internos. */
    public Map<String,Object> informacionAsignada(Long id,String actor){
        var v=repo.findById(id).orElseThrow(()->new RolNoAutorizadoException("La información de esta asignación no está disponible"));
        if(!actor.equals(v.getFamilia().getRegistradoPor()) || !accesoInfantilFamiliar(v))throw new RolNoAutorizadoException("El acceso a la información de esta asignación no está disponible");
        var n=v.getNino();Map<String,Object> r=new LinkedHashMap<>();r.put("vinculacion", "VIN-"+v.getId());
        r.put("edad",n.getEdad());r.put("unidadEdad",n.getUnidadEdad());r.put("nivelEscolar",n.getNivelEscolar());r.put("rendimientoAcademico",n.getRendimientoAcademico());
        r.put("alergias",n.getAlergias());r.put("intoleranciaLactosa",n.getIntoleranciaLactosa());r.put("condicionesSalud",n.getCondicionesSalud());
        r.put("habilidades",n.getHabilidades());r.put("gustos",n.getGustos());r.put("personalidad",n.getPersonalidad());
        return r;
    }
    public Map<String, Object> antecedenteAdopcion(Long familiaId) {
        return repo.findFirstByFamiliaIdAndResultadoCierreOrderByFechaRegistroCierreDescIdDesc(familiaId, ResultadoCierre.ADOPCION_FORMALIZADA)
                .map(v -> Map.<String, Object>of("vinculacionId", v.getId(), "reincorporacion", reincorporaciones.estado(v)))
                .orElseGet(Map::of);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> antecedentesFamilia(String actor) {
        List<Map<String, Object>> antecedentes = new ArrayList<>();
        for (var v : repo.findByFamiliaRegistradoPorOrderByIdDesc(actor)) {
            if (v.getEstado() != Vinculacion.Estado.CERRADA) continue;
            var registro = new LinkedHashMap<String, Object>();
            if (v.getResultadoCierre() == ResultadoCierre.REVOCACION_ASIGNACION) {
                var incidencia = bloqueante(incidencias(v));
                registro.put("tipo", "REVOCACION");
                registro.put("motivo", incidencia == null ? null : incidencia.getMotivo());
                registro.put("detalle", incidencia == null ? v.getMotivoCierre() : incidencia.getJustificacion());
            } else {
                var interrupcion = interrupcion(v);
                if (interrupcion == null) continue;
                registro.put("tipo", "INTERRUPCION");
                registro.put("motivo", interrupcion.getMotivo());
                registro.put("detalle", interrupcion.getJustificacion());
            }
            registro.put("vinculacionId", v.getId());
            registro.put("revision", v.isFamiliaLiberada() ? "CONFIRMADA" : v.isReservaVigente() ? "PENDIENTE" : null);
            antecedentes.add(registro);
        }
        return antecedentes;
    }
    public List<Map<String,Object>> propias(String actor){
        return repo.findByFamiliaRegistradoPorOrderByIdDesc(actor).stream().map(v->vista(v,true)).toList();
    }
    public List<Map<String,Object>> listar(String rol,String actor,String estado,String buscar){
        institucional(rol);
        return repo.findAllByOrderByIdDesc().stream().filter(v->autorizado(v,rol,actor))
            .filter(v->estado==null || estado.isBlank() || estadoVisible(v).name().equals(estado))
            .filter(v->buscar==null || ("VIN-"+v.getId()+" "+v.getNino().getCodigoCaso()+" "+v.getFamilia().getCodigoCaso()).toLowerCase(Locale.ROOT).contains(buscar.toLowerCase(Locale.ROOT)))
            .map(v -> {
                var resumen = resumen(v);
                resumen.put("maximoAplicableOriginal", compatibilidadOriginal(v.getDecision()).get("maximoAplicable"));
                return resumen;
            }).toList();
    }
    public Map<String,Object> detalle(Long id,String rol,String actor){
        institucional(rol);
        Vinculacion v=repo.findById(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(!autorizado(v,rol,actor))throw new RolNoAutorizadoException("No tienes acceso a esta vinculación");
        var r=vista(v,false);
        var interrupcion=interrupcion(v);
        var incidencias = incidencias(v);
        var bloqueante = bloqueante(incidencias);
        var resultados = cierreDisponible(v, interrupcion, bloqueante);
        boolean comite = "COMITE".equals(rol);
        if (comite || "TRABAJADOR_SOCIAL".equals(rol)) {
            r.put("incidenciasAsignacion", incidencias.stream().map(this::datosIncidencia).toList());
            r.put("puedeReportarIncidencia", "TRABAJADOR_SOCIAL".equals(rol) && enAcompanamiento(v) && bloqueante == null);
            r.put("puedeRevisarIncidencia", comite && enAcompanamiento(v) && bloqueante != null && bloqueante.getFechaRevision() == null);
        }
        r.put("estadoCierre", v.getEstado() == Vinculacion.Estado.CERRADA ? "REGISTRADO"
                : v.getEstado() != Vinculacion.Estado.RESPUESTA_ACEPTADA ? null
                : (interrupcion != null && interrupcion.getFechaRevision() == null)
                  || (bloqueante != null && bloqueante.getFechaRevision() == null) ? "REVISION"
                : interrupcion != null || bloqueante != null ? "CIERRE" : "CURSO");
        if (comite && resultados.contains(ResultadoCierre.ADOPCION_FORMALIZADA)) {
            var tiempos = TiemposVinculacion.calcular(v, "ACEPTADA", false, false, null);
            r.put("indicadoresAdopcionPendientes", tiempos.convivencia() == null
                    || tiempos.convivencia() < TiemposVinculacion.DIAS_REFERENCIA_ACOGIMIENTO
                    || visitasDomiciliarias.countByVinculacionId(v.getId()) < TiemposVinculacion.VISITAS_REFERENCIA_ACOGIMIENTO);
        }
        r.put("compatibilidadOriginal",compatibilidadOriginal(v.getDecision()));
        r.put("interrupcion",interrupcion==null?null:detalleInterrupcion(interrupcion));
        r.put("puedeRegistrarAutorizacion","COMITE".equals(rol) && v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA && v.isReservaVigente() && interrupcion==null && v.getFechaRegistroAutorizacion()==null && !v.getNino().isCierrePorAdopcion() && bloqueante == null);
        r.put("puedeCerrar","COMITE".equals(rol) && !resultados.isEmpty());
        r.put("puedeRevisarDisponibilidad","COMITE".equals(rol) && v.getEstado()==Vinculacion.Estado.CERRADA && v.isReservaVigente() && v.getResultadoCierre()!=ResultadoCierre.ADOPCION_FORMALIZADA);
        r.put("resultadosCierre", resultados);
        r.put("cierre",v.getEstado()==Vinculacion.Estado.CERRADA?datosCierre(v):null);
        r.put("puedeResolverReincorporacion", "COMITE".equals(rol) && v.getResultadoCierre() == ResultadoCierre.ADOPCION_FORMALIZADA);
        r.put("puedeReportarInterrupcion","TRABAJADOR_SOCIAL".equals(rol) && interrupcion==null && reportable(v));
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
    boolean autorizado(Vinculacion v,String rol,String actor){return "COMITE".equals(rol) || "TRABAJADOR_SOCIAL".equals(rol)&&actor.equals(v.getNino().getTrabajadorSocialAsignado()) || "PSICOLOGO".equals(rol)&&actor.equals(v.getNino().getPsicologoAsignado());}
    private Map<String,Object> resumen(Vinculacion v){
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",v.getId());r.put("codigo","VIN-"+v.getId());r.put("expedienteInfantil",v.getNino().getCodigoCaso());r.put("expedienteFamiliar",v.getFamilia().getCodigoCaso());r.put("fechaCreacion",fecha(v.getDecision().getFecha()));r.put("estado",estadoVisible(v));r.put("resultadoCierre",v.getResultadoCierre());r.put("puntajeOriginal",v.getDecision().getPuntaje());r.put("versionOriginal",v.getDecision().getVersionPesos());var i=interrupcion(v);r.put("interrupcionPendiente",i!=null && i.getFechaRevision()==null);r.put("interrupcionRegistrada",i!=null);return r;
    }
    private Map<String,Object> vista(Vinculacion v,boolean familiar){
        var r=resumen(v);
        // El portal usa una proyección separada; esta respuesta no incluye la entidad infantil.
        if(familiar){r.put("puedeConsultarInformacion",accesoInfantilFamiliar(v));r.remove("expedienteInfantil");r.remove("puntajeOriginal");r.remove("versionOriginal");}
        if(v.getFechaRegistroAutorizacion()!=null){
            var a=new LinkedHashMap<String,Object>();a.put("fechaEmision",v.getFechaEmisionAutorizacion());a.put("fechaInicioConvivencia",v.getFechaInicioConvivencia());a.put("registradaEl",fecha(v.getFechaRegistroAutorizacion()));a.put("inicioRegistradoEl",fecha(v.getFechaRegistroInicio()));
            if(!familiar){a.put("referencia",v.getReferenciaAutorizacion());a.put("responsable",v.getAutorizacionRegistradaPor());a.put("rol",v.getAutorizacionRol());a.put("inicioResponsable",v.getInicioRegistradoPor());a.put("inicioRol",v.getInicioRol());}r.put("autorizacionPreadoptiva",a);
        }
        if(v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA)r.put("respuestaFamiliar","ACEPTADA");
        if(Set.of(Vinculacion.Estado.RECHAZADA_POR_FAMILIA,Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE).contains(v.getEstado()))r.put("respuestaFamiliar","RECHAZADA");
        r.put("fechaRespuesta",fecha(v.getFechaRespuesta()));r.put("disponibilidad",!v.isReservaVigente()?"Pendiente de confirmar elegibilidad para Match":v.getEstado()==Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE?"Reservado; pendiente de confirmación institucional":"Reservado por asignación vigente");
        r.put("siguientePaso",switch(v.getEstado()){
            case RECHAZADA_POR_FAMILIA -> "Tu respuesta quedó registrada. Gracias por revisar la propuesta. El equipo correspondiente podrá continuar con el seguimiento de tu expediente.";
            case CERRADA -> "La vinculación está cerrada.";
            case PENDIENTE_RESPUESTA -> "Esta vinculación está pendiente de respuesta familiar.";
            case RESPUESTA_ACEPTADA -> "La familia aceptó la propuesta. El proceso continuará conforme a las actuaciones institucionales correspondientes.";
            case RESPUESTA_RECHAZADA_PENDIENTE_CIERRE -> "La familia rechazó la propuesta. El equipo debe confirmar el cierre y la disponibilidad de los expedientes.";
        });
        if(familiar)r.put("siguientePaso",switch(v.getEstado()){
            case RECHAZADA_POR_FAMILIA -> "Tu respuesta quedó registrada. Gracias por revisar la propuesta. El equipo correspondiente podrá continuar con el seguimiento de tu expediente.";
            case CERRADA -> "La vinculación está cerrada.";
            case PENDIENTE_RESPUESTA -> "Tienes una propuesta pendiente. Revisa la información disponible antes de responder.";
            case RESPUESTA_ACEPTADA -> "Aceptaste la propuesta. El equipo correspondiente continuará con el proceso.";
            case RESPUESTA_RECHAZADA_PENDIENTE_CIERRE -> "Rechazaste la propuesta. Tu respuesta quedó registrada. El equipo revisará el cierre institucional.";
        });
        if (v.getEstado() == Vinculacion.Estado.CERRADA && v.getResultadoCierre() != ResultadoCierre.ADOPCION_FORMALIZADA) {
            String estadoFamiliar = disponibilidadFamiliar(v);
            if (familiar) {
                r.put("estadoDisponibilidadFamiliar", estadoFamiliar);
                r.put("familiaDisponible", "DISPONIBLE".equals(estadoFamiliar));
            }
            r.put("disponibilidad", familiar ? estadoFamiliar
                    : "Consulta el estado de cada expediente en el cierre institucional.");
        }
        if (v.getResultadoCierre() == ResultadoCierre.ADOPCION_FORMALIZADA) {
            r.put("fechaCierre", v.getFechaCierre());
            r.put("reincorporacion", reincorporaciones.estado(v));
            r.put("siguientePaso", "La vinculación finalizó por adopción formalizada.");
        }
        if (v.getResultadoCierre() == ResultadoCierre.REVOCACION_ASIGNACION) {
            r.put("siguientePaso", "La asignación fue revocada.");
            if (familiar) {
                var incidencia = bloqueante(incidencias(v));
                var motivo = incidencia == null ? IncidenciaAsignacion.Motivo.OTRO : incidencia.getMotivo();
                r.put("motivoRevocacion", switch (motivo) {
                    case DESISTIMIENTO_FAMILIA -> "La familia decidió no continuar con la asignación.";
                    case RECHAZO_NINO -> "Durante el acompañamiento se determinó que la asignación no podía continuar.";
                    case OTRO -> "El Comité determinó que no correspondía continuar con la asignación tras revisar la situación registrada.";
                });
            }
        }
        var seguimiento=interrupcion(v);
        if(seguimiento!=null && v.getEstado()!=Vinculacion.Estado.CERRADA)r.put("siguientePaso",familiar
                ?"El equipo institucional te orientará sobre los siguientes pasos de esta vinculación."
                :seguimiento.getFechaRevision()==null?"La interrupción de convivencia está pendiente de revisión del Comité.":"El Comité registró su revisión. Los expedientes conservan su reserva hasta la actuación institucional correspondiente.");
        if(rechazoPortal(v)){
            r.put("siguientePaso",familiar?"Tu respuesta quedó registrada. Gracias por revisar la propuesta. El equipo correspondiente podrá continuar con el seguimiento de tu expediente.":"La propuesta del portal fue rechazada. Esta combinación queda fuera de las recomendaciones automáticas, sin cambiar la idoneidad de los expedientes.");
            r.put("disponibilidad","Sin reserva por esta propuesta; se mantienen los requisitos vigentes de cada expediente.");
        }
        {
            // Solo fechas y respuesta del caso propio; no se exponen informes ni justificaciones internas.
            Map<String, Object> seguimientoFamiliar = new LinkedHashMap<>();
            if (seguimiento != null) {
                seguimientoFamiliar.put("interrupcionRegistradaEl", fecha(seguimiento.getFechaRegistro()));
                seguimientoFamiliar.put("revisionRegistradaEl", fecha(seguimiento.getFechaRevision()));
                seguimientoFamiliar.put("interrupcionRol", seguimiento.getRolProfesional());
                seguimientoFamiliar.put("fechaInterrupcion", seguimiento.getFechaInterrupcion());
            }
            seguimientoFamiliar.put("cierreRegistradoEl", fecha(v.getFechaRegistroCierre()));
            if (familiar && v.isFamiliaLiberada()) {
                auditoria.findByCodigoCasoOrderByFechaAsc(v.getNino().getCodigoCaso()).stream()
                    .filter(e -> "DISPONIBILIDAD_REVISADA".equals(e.getAccion()))
                    .filter(e -> {
                        var datos = detalleEvento(e);
                        return datos.path("vinculacionId").asLong(-1) == v.getId()
                                && "FAMILIA".equals(datos.path("expediente").asText());
                    })
                    .reduce((anterior, ultimo) -> ultimo)
                    .ifPresent(e -> seguimientoFamiliar.put("disponibilidadRevisadaEl", fecha(e.getFecha())));
            }
            r.put("seguimientoFamiliar", seguimientoFamiliar);
            if (v.getEstado() == Vinculacion.Estado.CERRADA && v.getFechaRespuesta() != null) {
                auditoria.findByCodigoCasoOrderByFechaAsc(v.getNino().getCodigoCaso()).stream()
                    .filter(e -> "RESPUESTA_FAMILIAR".equals(e.getAccion()))
                    .filter(e -> detalleEvento(e).path("vinculacionId").asLong(-1) == v.getId())
                    .map(e -> detalleEvento(e).path("estado").asText(""))
                    .forEach(estadoRespuesta -> {
                        if ("RESPUESTA_ACEPTADA".equals(estadoRespuesta)) {
                            r.put("respuestaFamiliar", "ACEPTADA");
                        } else if (Set.of("RECHAZADA_POR_FAMILIA", "RESPUESTA_RECHAZADA_PENDIENTE_CIERRE")
                                .contains(estadoRespuesta)) {
                            r.put("respuestaFamiliar", "RECHAZADA");
                        }
                    });
            }
        }
        r.put("tiempos", TiemposVinculacion.calcular(v, (String) r.get("respuestaFamiliar"),
                r.get("estado") == Vinculacion.Estado.RECHAZADA_POR_FAMILIA, seguimiento != null,
                seguimiento == null ? null : seguimiento.getFechaInterrupcion() != null
                        ? seguimiento.getFechaInterrupcion()
                        : TiemposVinculacion.fechaCivil(seguimiento.getFechaRegistro())));
        if(!familiar){r.put("decisionComite",Map.of("decision",v.getDecision().getDecision(),"responsable",v.getDecision().getFuncionario(),"fecha",fecha(v.getDecision().getFecha()),"motivo",v.getDecision().getMotivo()));r.put("respondidoPor",v.getRespondidoPor());}
        return r;
    }
    @Transactional
    public Map<String,Object> registrarAutorizacion(Long id,String rol,String actor,com.senniaf.match.dto.AutorizacionPreadoptivaRequest r){
        if(!"COMITE".equals(rol))throw new RolNoAutorizadoException("Solo el Comité registra la constancia de autorización institucional");
        var v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(v.getEstado()!=Vinculacion.Estado.RESPUESTA_ACEPTADA || !v.isReservaVigente() || v.getNino().isCierrePorAdopcion() || interrupcion(v)!=null)
            throw new IllegalStateException("Se requiere una propuesta aceptada y vigente, sin interrupción ni cierre");
        if (bloqueante(incidencias(v)) != null)
            throw new IllegalStateException("Resuelve primero la incidencia de asignación registrada");
        if(r==null || !Boolean.TRUE.equals(r.confirmacion()) || r.fechaEmision()==null)throw new IllegalArgumentException("Confirma la autorización institucional y su fecha de emisión");
        String referencia=texto(r.referencia(),5,200,"Identifica la resolución o autorización institucional existente");
        var hoy=java.time.LocalDate.now(ZoneId.of("America/Panama"));
        if(r.fechaEmision().isAfter(hoy))throw new IllegalArgumentException("La fecha de emisión no puede ser futura");
        if (v.getFechaRegistroAutorizacion() != null)
            throw new IllegalStateException("La autorización y el inicio de convivencia ya se registraron en una sola actuación");
        var inicio = r.fechaInicioConvivencia() == null ? r.fechaEmision() : r.fechaInicioConvivencia();
        if (inicio.isAfter(hoy) || inicio.isBefore(r.fechaEmision()))
            throw new IllegalArgumentException("El inicio efectivo debe estar entre la emisión de la autorización y hoy");
        var ahora = LocalDateTime.now();
        v.setReferenciaAutorizacion(referencia);
        v.setFechaEmisionAutorizacion(r.fechaEmision());
        v.setAutorizacionRegistradaPor(actor);
        v.setAutorizacionRol(rol);
        v.setFechaRegistroAutorizacion(ahora);
        v.setFechaInicioConvivencia(inicio);
        v.setInicioRegistradoPor(actor);
        v.setInicioRol(rol);
        v.setFechaRegistroInicio(ahora);
        repo.saveAndFlush(v);
        auditar(v, actor, "AUTORIZACION_PREADOPTIVA_REGISTRADA",
                "El Comité registró en una sola actuación la autorización institucional y el inicio de convivencia.");
        return detalle(id,rol,actor);
    }
    @Transactional
    public Map<String,Object> registrarInterrupcion(Long id,String rol,String actor,com.senniaf.match.dto.InterrupcionRequest solicitud){
        if(!"TRABAJADOR_SOCIAL".equals(rol))throw new RolNoAutorizadoException("Solo el trabajador social asignado puede registrar la interrupción");
        Vinculacion v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(!autorizado(v,rol,actor))throw new RolNoAutorizadoException("No tienes asignada esta vinculación");
        if(!reportable(v))throw new IllegalStateException("Esta vinculación no permite registrar una interrupción de convivencia");
        if(interrupcion(v)!=null)throw new IllegalStateException("Esta vinculación ya tiene una interrupción registrada. Consulta su revisión");
        LocalDateTime ahora=LocalDateTime.now();
        if(solicitud==null || solicitud.motivo()==null)throw new IllegalArgumentException("Selecciona el motivo de la interrupción");
        InterrupcionConvivencia i=new InterrupcionConvivencia();i.setVinculacion(v);i.setMotivo(solicitud.motivo());
        // Fecha de registro e identidad proceden del servidor; no inventar fecha del hecho ni referencia.
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
        var resultado = solicitud.resultado();
        if (resultado == null || !cierreDisponible(v, interrupcion(v), bloqueante(incidencias(v))).contains(resultado))
            throw new IllegalStateException("El resultado seleccionado no corresponde a las actuaciones registradas");
        if (resultado == ResultadoCierre.ADOPCION_FORMALIZADA) {
            var nino = ninos.bloquearPorId(v.getNino().getId()).orElseThrow();
            familias.bloquearPorId(v.getFamilia().getId()).orElseThrow();
            if (nino.isCierrePorAdopcion() || nino.isBajaLogica()
                    || solicitudesInfantiles.existsByExpedienteIdAndEstado(nino.getId(), SolicitudEstadoCaso.Estado.PENDIENTE))
                throw new IllegalStateException("Resuelve primero la situación o solicitud pendiente del expediente infantil");
            nino.setCierrePorAdopcion(true);
            nino.setBajaLogica(true);
        }
        if (resultado == ResultadoCierre.REVOCACION_ASIGNACION
                && !excluidas.existsByCodigoNinoAndCodigoFamilia(v.getNino().getCodigoCaso(), v.getFamilia().getCodigoCaso())) {
            var exclusion = new CombinacionExcluida();
            exclusion.codigoNino = v.getNino().getCodigoCaso();
            exclusion.codigoFamilia = v.getFamilia().getCodigoCaso();
            exclusion.origen = "REVOCACION";
            exclusion.responsable = actor;
            exclusion.fecha = LocalDateTime.now();
            excluidas.save(exclusion);
        }
        v.setResultadoCierre(resultado);
        var ahora=LocalDateTime.now();
        v.setMotivoCierre(texto(solicitud.motivo(),10,2000,"Explica el motivo o resultado del cierre"));
        v.setFechaCierre(fechaPanama(ahora));v.setCerradoPor(actor);v.setFechaRegistroCierre(ahora);v.setEstado(Vinculacion.Estado.CERRADA);repo.save(v);
        auditar(v,actor,"CIERRE_INSTITUCIONAL",switch (resultado) {
            case ADOPCION_FORMALIZADA -> "El Comité registró el cierre por adopción formalizada. El expediente infantil quedó retirado definitivamente.";
            case INTERRUPCION_CONVIVENCIA -> "El Comité registró el cierre por interrupción revisada. Las disponibilidades se revisan por separado.";
            case REVOCACION_ASIGNACION -> "El Comité registró la revocación de asignación. Las disponibilidades se revisan por separado.";
            case CIERRE_DOCUMENTADO -> throw new IllegalStateException("Este resultado se conserva únicamente como antecedente");
        });
        return detalle(id,rol,actor);
    }
    private Set<ResultadoCierre> cierreDisponible(Vinculacion v, InterrupcionConvivencia interrupcion,
                                                  IncidenciaAsignacion incidencia) {
        if (v.getEstado() != Vinculacion.Estado.RESPUESTA_ACEPTADA || !v.isReservaVigente()) return Set.of();
        if (interrupcion != null) return interrupcion.getFechaRevision() == null
                ? Set.of() : Set.of(ResultadoCierre.INTERRUPCION_CONVIVENCIA);
        if (enAcompanamiento(v) && incidencia != null && Boolean.TRUE.equals(incidencia.getProcedeRevocacion()))
            return Set.of(ResultadoCierre.REVOCACION_ASIGNACION);
        return reportable(v) && v.getFechaRegistroInicio() != null
                && !v.getFechaInicioConvivencia().isAfter(java.time.LocalDate.now(TiemposVinculacion.PANAMA))
                ? Set.of(ResultadoCierre.ADOPCION_FORMALIZADA) : Set.of();
    }
    private boolean enAcompanamiento(Vinculacion v) {
        return v.getEstado() == Vinculacion.Estado.RESPUESTA_ACEPTADA && v.isReservaVigente()
                && v.getFechaRespuesta() != null && v.getFechaRegistroAutorizacion() == null
                && v.getFechaInicioConvivencia() == null && interrupcion(v) == null;
    }

    private List<IncidenciaAsignacion> incidencias(Vinculacion v) {
        return incidenciasAsignacion == null ? List.of() : incidenciasAsignacion.findByVinculacionIdOrderByIdDesc(v.getId());
    }

    private IncidenciaAsignacion bloqueante(List<IncidenciaAsignacion> incidencias) {
        return incidencias.stream().filter(i -> !Boolean.FALSE.equals(i.getProcedeRevocacion())).findFirst().orElse(null);
    }

    private Map<String, Object> datosIncidencia(IncidenciaAsignacion i) {
        var r = new LinkedHashMap<String, Object>();
        r.put("id", i.getId());
        r.put("motivo", i.getMotivo());
        r.put("justificacion", i.getJustificacion());
        r.put("registradoPor", i.getRegistradoPor());
        r.put("fechaRegistro", fecha(i.getFechaRegistro()));
        r.put("procedeRevocacion", i.getProcedeRevocacion());
        r.put("fundamentoRevision", i.getFundamentoRevision());
        r.put("revisadoPor", i.getRevisadoPor());
        r.put("fechaRevision", fecha(i.getFechaRevision()));
        return r;
    }

    @Transactional
    public Map<String, Object> registrarIncidencia(Long id, String rol, String actor, IncidenciaAsignacion.Motivo motivo,
                                                   String justificacion) {
        if (!"TRABAJADOR_SOCIAL".equals(rol))
            throw new RolNoAutorizadoException("Solo Trabajo Social puede registrar incidencias de asignación");
        var v = repo.bloquear(id).orElseThrow(() -> new IllegalArgumentException("La vinculación no está disponible"));
        if (!autorizado(v, rol, actor)) throw new RolNoAutorizadoException("No tienes asignada esta vinculación");
        if (!enAcompanamiento(v) || bloqueante(incidencias(v)) != null)
            throw new IllegalStateException("La etapa o una incidencia vigente impiden un nuevo registro");
        if (motivo == null) throw new IllegalArgumentException("Selecciona el motivo de la incidencia");
        incidenciasAsignacion.saveAndFlush(new IncidenciaAsignacion(v, motivo,
                texto(justificacion, 10, 2000, "Fundamenta la incidencia en 10 a 2000 caracteres"), actor));
        auditar(v, actor, "INCIDENCIA_ASIGNACION_REGISTRADA", "Trabajo Social registró una incidencia de asignación pendiente de revisión.");
        return detalle(id, rol, actor);
    }

    @Transactional
    public Map<String, Object> revisarIncidencia(Long id, Long incidenciaId, String rol, String actor,
                                                boolean procede, String fundamento) {
        exigirComite(rol);
        var v = repo.bloquear(id).orElseThrow(() -> new IllegalArgumentException("La vinculación no está disponible"));
        var i = bloqueante(incidencias(v));
        if (!enAcompanamiento(v) || i == null || !i.getId().equals(incidenciaId) || i.getFechaRevision() != null)
            throw new IllegalStateException("La incidencia ya no está pendiente de revisión");
        i.revisar(procede, texto(fundamento, 10, 2000, "Fundamenta la revisión en 10 a 2000 caracteres"), actor);
        auditar(v, actor, "INCIDENCIA_ASIGNACION_REVISADA", procede
                ? "El Comité confirmó que procede la revocación. Los expedientes conservan sus reservas."
                : "El Comité determinó que la revocación no procede. La vinculación puede continuar.");
        return detalle(id, rol, actor);
    }
    @Transactional
    public Map<String,Object> revisarDisponibilidad(Long id,String rol,String actor,String expediente,String justificacion){
        exigirComite(rol);
        if(!Set.of("NINO","FAMILIA").contains(expediente==null?"":expediente))throw new IllegalArgumentException("Selecciona el expediente que se ha revisado");
        var v=repo.bloquear(id).orElseThrow(()->new IllegalArgumentException("La vinculación no está disponible"));
        if(v.getEstado()!=Vinculacion.Estado.CERRADA)throw new IllegalStateException("Primero debe registrarse el cierre institucional");
        if (v.getResultadoCierre() == ResultadoCierre.ADOPCION_FORMALIZADA)
            throw new IllegalStateException("La adopción formalizada exige el trámite de reincorporación exclusivamente familiar");
        if (!v.isReservaVigente())
            throw new IllegalStateException("Esta vinculación no conserva una reserva pendiente de revisión");
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
        var r=new LinkedHashMap<String,Object>();r.put("fecha",v.getFechaCierre());r.put("referencia",v.getReferenciaCierre());r.put("autoridad",v.getAutoridadCierre());r.put("motivo",v.getMotivoCierre());r.put("registradoPor",v.getCerradoPor());r.put("registradoEl",fecha(v.getFechaRegistroCierre()));r.put("ninoLiberado",v.isNinoLiberado());r.put("familiaLiberada",v.isFamiliaLiberada());
        r.put("estadoInfantil", disponibilidadInfantil(v));
        r.put("estadoFamiliar", disponibilidadFamiliar(v));
        return r;
    }

    private String disponibilidadInfantil(Vinculacion v) {
        if (v.getNino().isCierrePorAdopcion()) return "RETIRADO_ADOPCION";
        if (!v.isNinoLiberado()) return v.isReservaVigente() ? "PENDIENTE_REVISION" : "SIN_CONSTANCIA";
        if (!v.getNino().isPerfilCompleto() || v.getNino().isBajaLogica()) return "NO_ELEGIBLE";
        return repo.existsByNinoCodigoCasoAndReservaVigenteTrue(v.getNino().getCodigoCaso()) ? "ASIGNADO" : "DISPONIBLE";
    }

    private String disponibilidadFamiliar(Vinculacion v) {
        if (!v.isFamiliaLiberada()) {
            if (v.getResultadoCierre() == ResultadoCierre.ADOPCION_FORMALIZADA) return "REINCORPORACION";
            return v.isReservaVigente() ? "PENDIENTE_REVISION" : "SIN_CONSTANCIA";
        }
        if (!v.getFamilia().isPerfilCompleto() || v.getFamilia().isReevaluacionPendiente()) return "NO_ELEGIBLE";
        return repo.existsByFamiliaCodigoCasoAndReservaVigenteTrue(v.getFamilia().getCodigoCaso()) ? "ASIGNADO" : "DISPONIBLE";
    }
    /** Solo lee el fundamento guardado; nunca invoca el cálculo actual. */
    private Map<String,Object> compatibilidadOriginal(DecisionComite d){
        var r=new LinkedHashMap<String,Object>();r.put("puntaje",d.getPuntaje());r.put("version",d.getVersionPesos());
        try{var c=d.getContextoScoring()==null?null:json.readTree(d.getContextoScoring());r.put("contexto",c!=null&&c.isObject()?c:null);r.put("maximoAplicable",ScoringService.maximoHistorico(c));}catch(com.fasterxml.jackson.core.JsonProcessingException error){r.put("contexto",null);}return r;
    }
    private boolean rechazoPortal(Vinculacion v){
        return v.getEstado()==Vinculacion.Estado.RECHAZADA_POR_FAMILIA || v.getEstado()==Vinculacion.Estado.RESPUESTA_RECHAZADA_PENDIENTE_CIERRE && !v.isReservaVigente() && v.getFechaCierre()==null && interrupcion(v)==null;
    }
    private Vinculacion.Estado estadoVisible(Vinculacion v){return rechazoPortal(v)?Vinculacion.Estado.RECHAZADA_POR_FAMILIA:v.getEstado();}
    boolean reportable(Vinculacion v){return v.getFechaRegistroAutorizacion()!=null && v.getFechaInicioConvivencia()!=null && v.isReservaVigente() && v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA && v.getFechaRespuesta()!=null && !v.getNino().isCierrePorAdopcion();}
    private InterrupcionConvivencia interrupcion(Vinculacion v){return interrupciones==null?null:interrupciones.findByVinculacionId(v.getId()).orElse(null);}
    private Map<String,Object> detalleInterrupcion(InterrupcionConvivencia i){
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",i.getId());r.put("fechaInterrupcion",i.getFechaInterrupcion());
        r.put("motivo",i.getMotivo());r.put("referenciaActuacion",i.getReferenciaActuacion());r.put("justificacion",i.getJustificacion());r.put("registradoPor",i.getRegistradoPor());r.put("rolProfesional",i.getRolProfesional());r.put("fechaRegistro",fecha(i.getFechaRegistro()));
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
