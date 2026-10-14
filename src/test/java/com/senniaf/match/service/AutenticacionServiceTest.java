package com.senniaf.match.service;
import com.senniaf.match.model.*;
import com.senniaf.match.repository.SesionAccesoRepository;
import com.senniaf.match.security.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AutenticacionServiceTest {
 @Test void unaCabeceraConUnRolFalsoNoAutorizaSinToken(){
  AutenticacionService auth=new AutenticacionService(mock(SesionAccesoRepository.class));
  assertThrows(AccesoNoAutorizadoException.class,()->auth.requerir("TRABAJADOR_SOCIAL","TRABAJADOR_SOCIAL"));
 }
 @Test void sesionFirmadaPorServidorRespetaElRolReal(){
  SesionAccesoRepository repo=mock(SesionAccesoRepository.class);
  when(repo.save(any())).thenAnswer(i->{SesionAcceso s=i.getArgument(0);when(repo.findById(s.getTokenHash())).thenReturn(Optional.of(s));return s;});
  AutenticacionService auth=new AutenticacionService(repo);
  Usuario f=new Usuario();f.setIdentificador("8-123-456");f.setRol("FAMILIA");f.setVerificado(true);
  String token=auth.iniciar(f);
  assertEquals(f,auth.requerir("Bearer "+token,"FAMILIA"));
  assertThrows(RolNoAutorizadoException.class,()->auth.requerir("Bearer "+token,"COMITE"));
 }
}
