package com.senniaf.match.service;
import com.senniaf.match.support.ServiciosDePrueba;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ScoringServiceTest {
 private ScoringService scoring;private PerfilNinoRepository ninos;private PerfilFamiliaRepository familias;
 private ConfiguracionScoringRepository config; private DecisionComiteRepository decisiones;
 @BeforeEach void setUp(){
  ninos=mock(PerfilNinoRepository.class);familias=mock(PerfilFamiliaRepository.class);
  config=mock(ConfiguracionScoringRepository.class);decisiones=mock(DecisionComiteRepository.class);
  scoring=new ScoringService(ninos,familias,config,decisiones,new ServiciosDePrueba.AuditoriaEnMemoria());
  when(config.findById(1L)).thenReturn(Optional.of(new ConfiguracionScoring()));
  when(decisiones.findByCodigoNinoOrderByFechaAscIdAsc(anyString())).thenReturn(List.of());
 }
 private PerfilNino nino(){
  PerfilNino n=new PerfilNino();n.setCodigoCaso("NNA-TEST");n.setEdad(8);n.setPerfilCompleto(true);
  n.setTieneHermanos(SiNo.SI);n.setCondicionesSalud(List.of(CondicionSalud.ASMA));return n;
 }
 private PerfilFamilia familia(String codigo,boolean dispuesto){
  PerfilFamilia f=new PerfilFamilia();f.setCodigoCaso(codigo);f.setPerfilCompleto(true);
  f.setEdadMinimaAceptada(5);f.setEdadMaximaAceptada(12);f.setDisposicionHermanos(dispuesto?DisposicionHermanos.DISPUESTA:DisposicionHermanos.PREFIERE_UN_SOLO_NINO);
  f.setCondicionesPuedeAcompanarHoy(dispuesto?List.of(CondicionSalud.ASMA):List.of());
  f.setCondicionesConApoyo(List.of());f.setRedApoyo(dispuesto?List.of(FuenteApoyo.PAREJA,FuenteApoyo.FAMILIA_EXTENDIDA):List.of(FuenteApoyo.NINGUNA_POR_AHORA));
  f.setDisponibilidadViaje(dispuesto?DisponibilidadViaje.ALTA:DisponibilidadViaje.BAJA);return f;
 }
 @Test void ordenaPerfilesCompletosSegunNecesidades(){
   when(ninos.findByCodigoCasoAndEliminadoFalse("NNA-TEST")).thenReturn(Optional.of(nino()));
   PerfilFamilia buena=familia("FAM-BUENA",true),baja=familia("FAM-BAJA",false),incompleta=familia("FAM-INCOMPLETA",true);
   incompleta.setPerfilCompleto(false);when(familias.findAll()).thenReturn(List.of(baja,incompleta,buena));
   ScoringService.Caso r=scoring.ranking("NNA-TEST");
   assertEquals(2,r.candidatos().size());assertEquals("FAM-BUENA",r.candidatos().get(0).codigoFamilia());
   assertEquals(100,r.candidatos().get(0).total());assertTrue(r.candidatos().get(1).total()<100);
   assertEquals(1,r.versionScoring());
 }
 @Test void rechazaPesosQueNoSumanCien(){
  assertThrows(IllegalArgumentException.class,()->scoring.cambiarPesos(Map.of("edad",40,"hermanos",25,"salud",35,"apoyo",15,"logistica",5),"CM-001"));
 }
 @Test void perfilIncompletoNoCalculaRanking(){
  PerfilNino n=nino();n.setPerfilCompleto(false);
  when(ninos.findByCodigoCasoAndEliminadoFalse("NNA-TEST")).thenReturn(Optional.of(n));
  assertTrue(scoring.ranking("NNA-TEST").candidatos().isEmpty());verify(familias,never()).findAll();
 }

 @Test void familiaYaAceptadaEnOtroCasoNoVuelveAlRanking(){
  when(ninos.findByCodigoCasoAndEliminadoFalse("NNA-TEST")).thenReturn(Optional.of(nino()));
  DecisionComite d=new DecisionComite();d.setCodigoNino("NNA-OTRO");d.setCodigoFamilia("FAM-OCUPADA");
  d.setDecision("ACEPTADA");
  when(decisiones.findByDecision("ACEPTADA")).thenReturn(List.of(d));
  when(familias.findAll()).thenReturn(List.of(familia("FAM-OCUPADA",true),familia("FAM-LIBRE",true)));
  var r=scoring.ranking("NNA-TEST");
  assertEquals(1,r.candidatos().size());
  assertEquals("FAM-LIBRE",r.candidatos().get(0).codigoFamilia());
 }
 @Test void noPermiteAceptarLaMismaFamiliaParaOtroNino(){
  when(decisiones.existsByCodigoFamiliaAndDecisionAndCodigoNinoNot("FAM-OCUPADA","ACEPTADA","NNA-TEST"))
   .thenReturn(true);
  var e=assertThrows(IllegalStateException.class,
   ()->scoring.decidir("NNA-TEST","FAM-OCUPADA","ACEPTADA","Razón profesional suficiente","CM-001"));
  assertTrue(e.getMessage().contains("otro caso"));
  verify(decisiones,never()).save(any());
 }
 @Test void decisionAceptadaSeGuardaParaConsultaPosterior(){
  when(ninos.findByCodigoCasoAndEliminadoFalse("NNA-TEST")).thenReturn(Optional.of(nino()));
  when(familias.findAll()).thenReturn(List.of(familia("FAM-OK",true)));
  when(decisiones.save(any(DecisionComite.class))).thenAnswer(inv->inv.getArgument(0));
  DecisionComite d=scoring.decidir("NNA-TEST","FAM-OK","ACEPTADA","Se verificaron los apoyos familiares","CM-001");
  assertEquals("ACEPTADA",d.getDecision());
  assertEquals("FAM-OK",d.getCodigoFamilia());
  verify(decisiones).save(d);
 }
}
