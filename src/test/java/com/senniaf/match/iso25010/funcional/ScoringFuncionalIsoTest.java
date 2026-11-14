package com.senniaf.match.iso25010.funcional;

import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.service.ScoringService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class ScoringFuncionalIsoTest {
  private ScoringService service;
  @BeforeEach void setup(){ service=new ScoringService(mock(PerfilNinoRepository.class),mock(PerfilFamiliaRepository.class),mock(DecisionComiteRepository.class),mock(PesosScoringRepository.class)); }
  private PerfilNino n(int edad, SiNo hermanos){ var n=new PerfilNino(); n.setCodigoCaso("N-01"); n.setEdad(edad); n.setTieneHermanos(hermanos); n.setCondicionesSalud(List.of(CondicionSalud.NINGUNA_CONOCIDA)); return n; }
  private PerfilFamilia f(int min,int max, DisponibilidadViaje viaje, DisposicionHermanos disp){ var f=new PerfilFamilia(); f.setCodigoCaso("F-01"); f.setEdadMinimaAceptada(min); f.setEdadMaximaAceptada(max); f.setDisponibilidadViaje(viaje); f.setDisposicionHermanos(disp); f.setRedApoyo(List.of(FuenteApoyo.PAREJA)); return f; }
  private PesosScoring p(){ return new PesosScoring(); }
  @Test void FUN01_noAcreditaHermanosNiSaludNoAplicables(){ var r=service.candidato(n(8,SiNo.NO),f(6,10,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA),p()); assertEquals(40,r.get("puntaje")); assertEquals(40,r.get("maximoAplicable")); }
  @ParameterizedTest(name="FUN-{index}: edad {0}") @ValueSource(ints={1,3,5,8,12,17}) void FUN02_edadDentroDelRango(int edad){ var r=service.candidato(n(edad,SiNo.NO),f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA),p()); assertEquals(40,r.get("puntaje")); }
  @ParameterizedTest(name="FUN-{index}: edad fuera {0}") @ValueSource(ints={0,18,25,40,60}) void FUN03_edadFueraNoSuma(int edad){ var r=service.candidato(n(edad,SiNo.NO),f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA),p()); assertEquals(20,r.get("puntaje")); }
  @ParameterizedTest(name="FUN-{index}: viaje {0}") @EnumSource(DisponibilidadViaje.class) void FUN04_logisticaPorDisponibilidad(DisponibilidadViaje v){ var r=service.candidato(n(8,SiNo.NO),f(1,17,v,DisposicionHermanos.DISPUESTA),p()); assertNotNull(r.get("f")); }
  @ParameterizedTest(name="FUN-{index}: hermanos {0}") @EnumSource(DisposicionHermanos.class)
  void FUN05_disposicionHermanos(DisposicionHermanos d){
    var nino=n(8,SiNo.SI);nino.setCantidadHermanos(1);
    var r=service.candidato(nino,f(1,17,DisponibilidadViaje.ALTA,d),p());
    int puntosHermanos=switch(d){case DISPUESTA -> 25;case DISPUESTA_CON_DUDAS -> 13;default -> 0;};
    assertEquals(40+puntosHermanos,r.get("puntaje"));
  }
  @Test void FUN06_sinHermanosNoAcreditaPuntos(){ var r=service.candidato(n(8,SiNo.NO),f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.PREFIERE_UN_SOLO_NINO),p()); assertEquals(40,r.get("puntaje")); assertEquals(40,r.get("maximoAplicable")); }
  @Test void FUN07_redApoyoSumaFactor(){ var r=service.candidato(n(8,SiNo.NO),f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA),p()); assertEquals(15,((List<?>)((Map<?,?>)r.get("f")).get("apoyo")).get(0)); }
  @Test void FUN08_familiaSinApoyoNoSuma(){ var f=f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA); f.setRedApoyo(List.of(FuenteApoyo.NINGUNA_POR_AHORA)); assertEquals(25,service.candidato(n(8,SiNo.NO),f,p()).get("puntaje")); }
  @Test void FUN09_alertaEnTresReevaluaciones(){ var f=f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA); f.setNumeroReevaluaciones(3); assertEquals(true,service.candidato(n(8,SiNo.NO),f,p()).get("alertaRevisionTecnica")); }
  @ParameterizedTest @ValueSource(ints={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}) void FUN10_puntajeNoNegativo(int edad){ assertTrue((int)service.candidato(n(edad,SiNo.NO),f(1,17,DisponibilidadViaje.BAJA,DisposicionHermanos.DISPUESTA),p()).get("puntaje")>=0); }
  @Test void FUN11_hermanosSinCantidadRequierenConfirmacion(){
    var r=service.candidato(n(8,SiNo.SI),f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA),p());
    assertEquals(40,r.get("puntaje"));assertEquals("DESCONOCIDO",((Map<?,?>)r.get("estadosFactores")).get("hermanos"));
  }
  @Test void FUN12_evidenciaCompletaPuedeAcreditarLosCincoPesos(){
    var nino=n(8,SiNo.SI);nino.setCantidadHermanos(1);nino.setCondicionesSalud(List.of(CondicionSalud.ASMA));
    var familia=f(1,17,DisponibilidadViaje.ALTA,DisposicionHermanos.DISPUESTA);
    familia.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA));familia.setCondicionesConApoyo(List.of());
    assertEquals(100,service.candidato(nino,familia,p()).get("puntaje"));
  }

}
