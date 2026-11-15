package com.senniaf.match.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class ProgramacionFechaTest {
 @Test void primeraFechaRespetaVeinticuatroHorasYHoraPanama(){
  assertEquals(Instant.parse("2026-11-16T12:00:00Z"),PesosProgramadosService.primeraFecha(Instant.parse("2026-11-14T14:00:00Z")));
  assertEquals(Instant.parse("2026-11-15T12:00:00Z"),PesosProgramadosService.primeraFecha(Instant.parse("2026-11-14T12:00:00Z")));
  assertEquals(Instant.parse("2026-11-16T12:00:00Z"),PesosProgramadosService.primeraFecha(Instant.parse("2026-11-14T12:00:00.000000001Z")));
  assertEquals(Instant.parse("2027-01-02T12:00:00Z"),PesosProgramadosService.primeraFecha(Instant.parse("2026-12-31T14:00:00Z")));
 }
}
