package com.senniaf.match.service;
import com.senniaf.match.model.*;
import com.senniaf.match.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class PesosProgramadosService {
 private final PesosScoringRepository pesos;
 private final ProgramacionPesosRepository programas;
 private final EventoCasoRepository auditoria;
 private final Clock reloj;
 private final com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 private PesosProgramadosService(PesosScoringRepository p,ProgramacionPesosRepository r,EventoCasoRepository a,Clock c){pesos=p;programas=r;auditoria=a;reloj=c;}
 public static Instant primeraFecha(Instant ahora){
  var minimo=ahora.plus(Duration.ofHours(24));var fecha=minimo.atZone(ZoneId.of("America/Panama")).toLocalDate();
  var prevista=fecha.atTime(7,0).atZone(ZoneId.of("America/Panama")).toInstant();
  return prevista.isBefore(minimo)?fecha.plusDays(1).atTime(7,0).atZone(ZoneId.of("America/Panama")).toInstant():prevista;
 }
 @Transactional public PesosScoring vigentes(){
  var p=pesos.bloquear().orElseThrow(()->new IllegalStateException("Falta la configuración inicial de criterios"));
  var pendiente=programas.findFirstByEstadoOrderByIdAsc("PENDIENTE").orElse(null);
  if(pendiente!=null && !pendiente.activarEl.isAfter(reloj.instant())){
   if(p.getVersion()!=pendiente.versionAnterior || !valores(p).equals(leer(pendiente.anteriores)))throw new IllegalStateException("La programación requiere revisión institucional antes de continuar");
   var v=leer(pendiente.nuevos);validar(v,pendiente.justificacion);p.setEdad(v.get("edad"));p.setHermanos(v.get("hermanos"));p.setSalud(v.get("salud"));p.setApoyo(v.get("apoyo"));p.setLogistica(v.get("logistica"));p.setVersion(pendiente.versionNueva);pesos.save(p);
   pendiente.estado="APLICADA";pendiente.activadaEl=reloj.instant();programas.save(pendiente);auditar(pendiente,"PESOS_ACTIVADOS");
  }
  return p;
 }
 @org.springframework.scheduling.annotation.Scheduled(fixedDelay=30000)
 public void comprobarActivaciones(){vigentesEnTransaccion();}
 private void vigentesEnTransaccion(){/* Abre una transacción para la activación periódica, sin depender de una petición web. */
  plantilla.executeWithoutResult(s->vigentes());
 }
 private org.springframework.transaction.support.TransactionTemplate plantilla;
 @org.springframework.beans.factory.annotation.Autowired
 public PesosProgramadosService(PesosScoringRepository p,ProgramacionPesosRepository r,EventoCasoRepository a,Clock c,org.springframework.transaction.PlatformTransactionManager tm){this(p,r,a,c);plantilla=new org.springframework.transaction.support.TransactionTemplate(tm);}
 @Transactional public Map<String,Object> programar(Map<String,Integer> valores,String actor,String razon,Integer version){
  validar(valores,razon);var p=vigentes();
  if(version==null||version!=p.getVersion())throw new IllegalStateException("Los criterios cambiaron mientras los revisabas. Recarga la página antes de confirmar");
  if(programas.findFirstByEstadoOrderByIdAsc("PENDIENTE").isPresent())throw new IllegalStateException("Ya existe una actualización pendiente. Revisa la programación actual antes de realizar otro cambio.");
  if(valores.equals(valores(p)))throw new IllegalArgumentException("No hay cambios por programar");
  var r=new ProgramacionPesos();r.responsable=actor;r.justificacion=razon.strip();r.anteriores=escribir(valores(p));r.nuevos=escribir(valores);r.versionAnterior=p.getVersion();r.versionNueva=p.getVersion()+1;r.programadaEl=reloj.instant();r.activarEl=primeraFecha(r.programadaEl);programas.saveAndFlush(r);auditar(r,"PESOS_PROGRAMADOS");return vista(r);
 }
 @Transactional public Map<String,Object> estado(){var p=vigentes();var r=new LinkedHashMap<String,Object>();r.put("versionVigente",p.getVersion());r.put("primeraActivacionValida",primeraFecha(reloj.instant()));r.put("pendiente",programas.findFirstByEstadoOrderByIdAsc("PENDIENTE").map(this::vista).orElse(null));return r;}
 public Map<String,Integer> valores(PesosScoring p){return Map.of("edad",p.getEdad(),"hermanos",p.getHermanos(),"salud",p.getSalud(),"apoyo",p.getApoyo(),"logistica",p.getLogistica());}
 public void validar(Map<String,Integer> v,String razon){
  if(razon==null||razon.strip().length()<10||razon.length()>300)throw new IllegalArgumentException("Explica el cambio de criterios en 10 a 300 caracteres");
  if(v==null||!v.keySet().equals(Set.of("edad","hermanos","salud","apoyo","logistica"))||v.values().stream().anyMatch(x->x==null||x<0||x>100)||v.values().stream().mapToInt(Integer::intValue).sum()!=100)throw new IllegalArgumentException("Los cinco pesos deben ser enteros entre 0 y 100 y sumar exactamente 100");
 }
 private Map<String,Object> vista(ProgramacionPesos p){var r=new LinkedHashMap<String,Object>();r.put("id",p.id);r.put("responsable",p.responsable);r.put("justificacion",p.justificacion);r.put("anteriores",leer(p.anteriores));r.put("nuevos",leer(p.nuevos));r.put("versionAnterior",p.versionAnterior);r.put("versionNueva",p.versionNueva);r.put("programadaEl",p.programadaEl);r.put("activarEl",p.activarEl);r.put("activadaEl",p.activadaEl);r.put("estado",p.estado);return r;}
 private void auditar(ProgramacionPesos p,String accion){var e=new EventoCaso();e.setCodigoCaso("CONFIG_MATCH");e.setActor("PESOS_ACTIVADOS".equals(accion)?"SISTEMA":p.responsable);e.setFecha(LocalDateTime.ofInstant(reloj.instant(),ZoneId.systemDefault()));e.setAccion(accion);e.setDescripcion(escribir(vista(p)));auditoria.save(e);}
 private String escribir(Object v){try{return json.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException("No se pudo conservar el registro del cambio",e);}}
 private Map<String,Integer> leer(String v){try{return json.readValue(v,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Integer>>(){});}catch(Exception e){throw new IllegalStateException("No se pudo leer la configuración registrada",e);}}
}
