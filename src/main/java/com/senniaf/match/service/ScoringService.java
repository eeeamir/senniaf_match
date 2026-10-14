package com.senniaf.match.service;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;
@Service
public class ScoringService {
 private final PerfilNinoRepository ninos;
 private final PerfilFamiliaRepository familias;
 private final ConfiguracionScoringRepository configuraciones;
 private final DecisionComiteRepository decisiones;
 private final AuditoriaService auditoria;
 public ScoringService(PerfilNinoRepository n,PerfilFamiliaRepository f,ConfiguracionScoringRepository c,DecisionComiteRepository d,AuditoriaService a){
  ninos=n;familias=f;configuraciones=c;decisiones=d;auditoria=a;
 }
 public record Factor(String clave,String nombre,int puntos,int maximo,String explicacion){}
 public record Candidato(String codigoFamilia,String provincia,int total,List<Factor> factores,List<String> alertas){}
 public record Caso(String codigoCaso,Integer edad,String escolaridad,boolean perfilCompleto,boolean alertaMedica,Long diasDesdeDeclaratoria,
         List<String> necesidades,int versionScoring,List<Candidato> candidatos,List<DecisionComite> decisiones){}
 @Transactional(readOnly=true)
 public List<Map<String,Object>> casos(){return ninos.findByEliminadoFalseOrderByFechaRegistroDesc().stream()
    .map(p->{Map<String,Object> m=new LinkedHashMap<>();m.put("codigoCaso",p.getCodigoCaso());m.put("edad",p.getEdad());
    m.put("perfilCompleto",p.isPerfilCompleto());m.put("alertaMedica",p.isAlertaMedica());return m;}).toList();}
 public ConfiguracionScoring pesos(){return configuraciones.findById(1L).orElseGet(ConfiguracionScoring::new);}
 @Transactional
 public synchronized ConfiguracionScoring cambiarPesos(Map<String,Integer> v, String actor){
   Set<String> claves=Set.of("edad","hermanos","salud","apoyo","logistica");
   if(v==null || !v.keySet().equals(claves) || v.values().stream().anyMatch(x->x==null||x<0||x>100)
      || v.values().stream().mapToInt(Integer::intValue).sum()!=100)
      throw new IllegalArgumentException("Indica los 5 pesos, cada uno entre 0 y 100, y que sumen 100.");
   ConfiguracionScoring c=pesos();c.setVersion(c.getVersion()+1);
   c.setEdad(v.get("edad"));c.setHermanos(v.get("hermanos"));c.setSalud(v.get("salud"));
   c.setApoyo(v.get("apoyo"));c.setLogistica(v.get("logistica"));configuraciones.save(c);
   auditoria.registrar(actor,"CONFIGURAR_SCORING","MOTOR","Versión "+c.getVersion()+"; pesos "+v);
   return c;
 }
 @Transactional(readOnly=true)
 public Caso ranking(String codigo){
    PerfilNino n=ninos.findByCodigoCasoAndEliminadoFalse(codigo).orElseThrow(()->new NoSuchElementException("Caso no encontrado."));
    ConfiguracionScoring pesos=pesos();
    List<Candidato> candidatos=n.isPerfilCompleto()?familias.findAll().stream().filter(PerfilFamilia::isPerfilCompleto)
       .map(f->calcular(n,f,pesos)).sorted(Comparator.comparingInt(Candidato::total).reversed().thenComparing(Candidato::codigoFamilia)).toList():List.of();
    Long dias=n.getFechaDeclaratoria()==null?null:ChronoUnit.DAYS.between(n.getFechaDeclaratoria().toLocalDate(),LocalDate.now());
    List<String> necesidades=new ArrayList<>();
    necesidades.add("Escolaridad: "+Objects.toString(n.getNivelEscolar(),"Sin dato")+" · grado: "+Objects.toString(n.getGradoEscolar(),"no registrado")+" · rendimiento: "+Objects.toString(n.getRendimientoAcademico(),"sin dato"));
    necesidades.add("Habilidades y facilidades: "+Objects.toString(n.getHabilidades(),"sin información")+". "+Objects.toString(n.getHabilidadesDetalle(),""));
    necesidades.add("Intereses: "+Objects.toString(n.getGustos(),"sin información")+". "+Objects.toString(n.getGustosDetalle(),""));
    necesidades.add("Personalidad y relaciones: "+Objects.toString(n.getPersonalidad(),"sin información")+". "+Objects.toString(n.getPersonalidadDetalle(),""));
    if(n.getNecesidadesApoyo()!=null) necesidades.add("Apoyos: "+n.getNecesidadesApoyo());
    if(n.getTieneHermanos()==SiNo.SI) necesidades.add("Relaciones con hermanos: "+Objects.toString(n.getHermanosDetalle(),"Evaluar conservación de vínculos"));
    if(n.isAlertaMedica()) necesidades.add("Revisión clínica específica requerida por el Comité");
    if(n.getContextoCultural()!=null && !n.getContextoCultural().isBlank()) necesidades.add("Continuidad cultural (revisión humana): "+n.getContextoCultural());
    return new Caso(n.getCodigoCaso(),n.getEdad(),n.getNivelEscolar()==null?"Sin dato":n.getNivelEscolar().name(),
       n.isPerfilCompleto(),n.isAlertaMedica(),dias,necesidades,pesos.getVersion(),candidatos,decisiones.findByCodigoNinoOrderByFechaAscIdAsc(codigo));
 }
 Candidato calcular(PerfilNino n, PerfilFamilia f, ConfiguracionScoring w){
   List<Factor> factores=new ArrayList<>();List<String> alertas=new ArrayList<>();
   boolean edadAceptada=n.getEdad()!=null && f.getEdadMinimaAceptada()!=null && f.getEdadMaximaAceptada()!=null &&
       n.getEdad()>=f.getEdadMinimaAceptada() && n.getEdad()<=f.getEdadMaximaAceptada();
   agregar(factores,"edad","Rango de edad",edadAceptada?1:0,w.getEdad(),"Familia indica rango de "+f.getEdadMinimaAceptada()+" a "+f.getEdadMaximaAceptada()+" años");
   if(!edadAceptada) alertas.add("La edad queda fuera del rango declarado; requiere conversación y evaluación.");
   double hermanos=n.getTieneHermanos()==SiNo.SI? switch(f.getDisposicionHermanos()){
     case DISPUESTA -> 1;case DISPUESTA_CON_DUDAS -> .55;case PREFIERE_UN_SOLO_NINO -> .1;
   }:1;
   agregar(factores,"hermanos","Vínculos entre hermanos",hermanos,w.getHermanos(),
     n.getTieneHermanos()==SiNo.SI ? "Hay hermanos; disposición familiar: "+f.getDisposicionHermanos() : "No se han reportado hermanos");
   if(n.getTieneHermanos()==SiNo.SI && hermanos<1)alertas.add("El Comité debe verificar cómo se preservarán los vínculos entre hermanos.");
   List<CondicionSalud> requeridas=n.getCondicionesSalud()==null?List.of():n.getCondicionesSalud().stream().filter(c->c!=CondicionSalud.NINGUNA_CONOCIDA).toList();
   Set<CondicionSalud> hoy=new HashSet<>(Objects.requireNonNullElse(f.getCondicionesPuedeAcompanarHoy(),List.of()));
   Set<CondicionSalud> conApoyo=new HashSet<>(Objects.requireNonNullElse(f.getCondicionesConApoyo(),List.of()));
   double capacidad=requeridas.isEmpty()?1:requeridas.stream().mapToDouble(c->hoy.contains(c)?1:conApoyo.contains(c)?.6:0).average().orElse(0);
   agregar(factores,"salud","Capacidad de acompañar condiciones",capacidad,w.getSalud(),
       requeridas.isEmpty()?"Sin condiciones específicas registradas":"Condiciones a revisar: "+requeridas);
   if(capacidad<1)alertas.add("La atención sanitaria o de desarrollo necesita verificación profesional, con los apoyos disponibles.");
   if(n.isAlertaMedica())alertas.add("Alergias e intolerancias deben revisarse aparte; no se infiere capacidad desde este puntaje.");
   int redes=(int)Objects.requireNonNullElse(f.getRedApoyo(),List.<FuenteApoyo>of()).stream().filter(x->x!=FuenteApoyo.NINGUNA_POR_AHORA).distinct().count();
   agregar(factores,"apoyo","Red de apoyo",Math.min(1,redes/2.0),w.getApoyo(),"Fuentes de apoyo declaradas: "+redes);
   double logistica=f.getDisponibilidadViaje()==DisponibilidadViaje.ALTA?1:f.getDisponibilidadViaje()==DisponibilidadViaje.MEDIA?.65:.3;
   agregar(factores,"logistica","Disponibilidad logística",logistica,w.getLogistica(),"Disponibilidad para desplazamientos: "+f.getDisponibilidadViaje());
   int total=factores.stream().mapToInt(Factor::puntos).sum();
   alertas.add("Contexto cultural, personalidad, intereses y trayectoria requieren evaluación cualitativa; no se convierten en puntaje.");
   return new Candidato(f.getCodigoCaso(),String.valueOf(f.getProvincia()),total,List.copyOf(factores),List.copyOf(alertas));
 }
 private void agregar(List<Factor> destino,String k,String n,double proporcion,int max,String descripcion){
     destino.add(new Factor(k,n,(int)Math.round(proporcion*max),max,descripcion));
 }
 @Transactional
 public DecisionComite decidir(String codigo,String familia,String decision,String motivo,String actor){
   if(!List.of("ACEPTADA","DESCARTADA","REEVALUAR").contains(decision))
      throw new IllegalArgumentException("Decisión no admitida.");
   if(motivo==null || motivo.trim().length()<10 || motivo.length()>2000)
      throw new IllegalArgumentException("Explica la decisión entre 10 y 2000 caracteres.");
   Caso r=ranking(codigo);
   Candidato candidato=r.candidatos().stream().filter(c->c.codigoFamilia().equals(familia)).findFirst()
      .orElseThrow(()->new IllegalArgumentException("La familia no es elegible para este ranking (perfil incompleto o inexistente)."));
   if(r.decisiones().stream().anyMatch(d->d.getDecision().equals("ACEPTADA")))
       throw new IllegalStateException("Ya hay una asignación aceptada para este caso.");
   List<DecisionComite> anteriores=r.decisiones().stream().filter(d->d.getCodigoFamilia().equals(familia)).toList();
   if(!anteriores.isEmpty() && !anteriores.get(anteriores.size()-1).getDecision().equals("REEVALUAR"))
      throw new IllegalStateException("Ya existe una decisión final para esa familia.");
   DecisionComite d=new DecisionComite();d.setCodigoNino(codigo);d.setCodigoFamilia(familia);
   d.setDecision(decision);d.setMotivo(motivo.trim());d.setActor(actor);d.setFecha(LocalDateTime.now());
   d.setPuntaje(candidato.total());d.setVersionScoring(r.versionScoring());
   d.setFactoresSnapshot(candidato.factores().stream().map(f->f.clave()+": "+f.puntos()+"/"+f.maximo()+" ("+f.explicacion()+")").collect(Collectors.joining(" | ")));
   decisiones.save(d);
   auditoria.registrar(actor,"DECISION_"+decision,codigo,"Familia "+familia+" · puntaje "+candidato.total()+" · versión "+r.versionScoring());
   return d;
 }
}
