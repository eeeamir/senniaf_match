package com.senniaf.match.service;
import com.senniaf.match.support.ServiciosDePrueba;
import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.repository.PerfilNinoRepository;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class EdicionCasosTest {
 @Test void soloResponsablePuedeConsultarEditarYEliminar(){
  PerfilNinoRepository repo=mock(PerfilNinoRepository.class);
  ServiciosDePrueba.AuditoriaEnMemoria auditoria=new ServiciosDePrueba.AuditoriaEnMemoria();
  PerfilNinoService service=new PerfilNinoService(repo,auditoria);
  PerfilNino caso=new PerfilNino();caso.setCodigoCaso("NNA-PRUEBA");caso.setRegistradoPor("TS-001");caso.setFechaRegistro(LocalDateTime.now());
  when(repo.findByCodigoCasoAndEliminadoFalse("NNA-PRUEBA")).thenReturn(Optional.of(caso));
  assertThrows(RolNoAutorizadoException.class,()->service.consultar("NNA-PRUEBA","TS-002"));
  assertThrows(RolNoAutorizadoException.class,()->service.editar("NNA-PRUEBA",new PerfilNinoRequest(),"TS-002"));
  assertThrows(RolNoAutorizadoException.class,()->service.eliminar("NNA-PRUEBA","TS-002"));
  service.eliminar("NNA-PRUEBA","TS-001");
  assertTrue(caso.isEliminado());
  assertTrue(auditoria.contiene("TS-001", "ELIMINAR_PERFIL", "NNA-PRUEBA"));
 }
}
